package com.wordmind.service;

import com.wordmind.dto.MindMapDTO;
import com.wordmind.entity.ReviewRecord;
import com.wordmind.entity.Word;
import com.wordmind.entity.WordRelation;
import com.wordmind.repository.ReviewRecordRepository;
import com.wordmind.repository.WordRelationRepository;
import com.wordmind.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MindMapService {
    
    @Autowired
    private WordRepository wordRepository;
    
    @Autowired
    private WordRelationRepository relationRepository;
    
    @Autowired
    private ReviewRecordRepository reviewRecordRepository;
    
    private static final Map<WordRelation.RelationType, Integer> RELATION_WEIGHTS = Map.of(
            WordRelation.RelationType.SYNONYM, 4,
            WordRelation.RelationType.ROOT, 3,
            WordRelation.RelationType.TOPIC, 2,
            WordRelation.RelationType.SCENE, 1,
            WordRelation.RelationType.ANTONYM, 1,
            WordRelation.RelationType.PREFIX, 1,
            WordRelation.RelationType.SUFFIX, 1
    );
    
    public MindMapDTO.Response getMindMap(Long wordId, int depth) {
        Word centerWord = wordRepository.findById(wordId)
                .orElseThrow(() -> new RuntimeException("单词不存在"));
        
        Set<Long> visitedWordIds = new HashSet<>();
        List<MindMapDTO.WordNode> nodes = new ArrayList<>();
        List<MindMapDTO.RelationEdge> edges = new ArrayList<>();
        
        // 添加中心节点
        MindMapDTO.WordNode centerNode = createNode(centerWord, 0);
        nodes.add(centerNode);
        visitedWordIds.add(centerWord.getId());
        
        // BFS遍历
        Queue<Word> queue = new LinkedList<>();
        queue.offer(centerWord);
        
        int currentDepth = 0;
        while (!queue.isEmpty() && currentDepth < depth) {
            int levelSize = queue.size();
            Set<Word> nextLevel = new HashSet<>();
            
            for (int i = 0; i < levelSize; i++) {
                Word current = queue.poll();
                List<WordRelation> relations = relationRepository.findByWordId(current.getId());
                
                for (WordRelation relation : relations) {
                    Long relatedWordId = relation.getSourceWordId().equals(current.getId()) 
                        ? relation.getTargetWordId() 
                        : relation.getSourceWordId();
                    
                    if (!visitedWordIds.contains(relatedWordId)) {
                        Word relatedWord = wordRepository.findById(relatedWordId).orElse(null);
                        if (relatedWord != null) {
                            MindMapDTO.WordNode node = createNode(relatedWord, currentDepth + 1);
                            nodes.add(node);
                            visitedWordIds.add(relatedWordId);
                            nextLevel.add(relatedWord);
                        }
                    }
                    
                    // 添加边
                    edges.add(createEdge(relation));
                }
            }
            
            queue.addAll(nextLevel);
            currentDepth++;
        }
        
        return MindMapDTO.Response.builder()
                .centerWord(centerNode)
                .nodes(nodes)
                .edges(edges)
                .build();
    }
    
    public MindMapDTO.LearningPathResponse getLearningPath(Long wordId, int depth, Long userId) {
        // 1. 获取思维导图的所有节点和边
        MindMapDTO.Response mindMap = getMindMap(wordId, depth);
        
        // 2. 获取用户对这些单词的熟练度
        Map<Long, Integer> proficiencyMap = getProficiencyMap(userId, mindMap.getNodes());
        
        // 3. 创建带熟练度的节点
        List<MindMapDTO.PathWordNode> pathNodes = mindMap.getNodes().stream()
                .map(node -> {
                    int proficiency = proficiencyMap.getOrDefault(node.getId(), 0);
                    return MindMapDTO.PathWordNode.builder()
                            .id(node.getId())
                            .word(node.getWord())
                            .meaning(node.getMeaning())
                            .category(node.getCategory())
                            .proficiency(proficiency)
                            .mastered(proficiency >= 3)
                            .build();
                })
                .collect(Collectors.toList());
        
        // 4. 创建带权重的边
        List<MindMapDTO.PathEdge> pathEdges = mindMap.getEdges().stream()
                .map(edge -> {
                    WordRelation.RelationType type = WordRelation.RelationType.valueOf(edge.getRelationType());
                    int weight = RELATION_WEIGHTS.getOrDefault(type, 1);
                    return MindMapDTO.PathEdge.builder()
                            .source(edge.getSource())
                            .target(edge.getTarget())
                            .relationType(edge.getRelationType())
                            .label(edge.getLabel())
                            .weight(weight)
                            .build();
                })
                .collect(Collectors.toList());
        
        // 5. 找出未掌握的节点
        List<MindMapDTO.PathWordNode> unmasteredNodes = pathNodes.stream()
                .filter(node -> !node.getMastered())
                .collect(Collectors.toList());
        
        // 6. 生成推荐学习路径
        List<MindMapDTO.PathEdge> recommendedPath = generateRecommendedPath(
                pathNodes, pathEdges, proficiencyMap);
        
        return MindMapDTO.LearningPathResponse.builder()
                .nodes(pathNodes)
                .edges(pathEdges)
                .unmasteredNodes(unmasteredNodes)
                .recommendedPath(recommendedPath)
                .build();
    }
    
    private Map<Long, Integer> getProficiencyMap(Long userId, List<MindMapDTO.WordNode> nodes) {
        Map<Long, Integer> proficiencyMap = new HashMap<>();
        for (MindMapDTO.WordNode node : nodes) {
            reviewRecordRepository.findByUserIdAndWordId(userId, node.getId())
                    .ifPresent(record -> proficiencyMap.put(node.getId(), record.getProficiency()));
        }
        return proficiencyMap;
    }
    
    private List<MindMapDTO.PathEdge> generateRecommendedPath(
            List<MindMapDTO.PathWordNode> nodes,
            List<MindMapDTO.PathEdge> edges,
            Map<Long, Integer> proficiencyMap) {
        
        List<MindMapDTO.PathEdge> recommendedPath = new ArrayList<>();
        Set<Long> masteredWordIds = nodes.stream()
                .filter(MindMapDTO.PathWordNode::getMastered)
                .map(MindMapDTO.PathWordNode::getId)
                .collect(Collectors.toSet());
        
        Set<Long> unmasteredWordIds = nodes.stream()
                .filter(node -> !node.getMastered())
                .map(MindMapDTO.PathWordNode::getId)
                .collect(Collectors.toSet());
        
        if (masteredWordIds.isEmpty() || unmasteredWordIds.isEmpty()) {
            return recommendedPath;
        }
        
        // 使用Dijkstra算法寻找从已掌握单词到未掌握单词的最优路径
        // 构建邻接表
        Map<Long, List<MindMapDTO.PathEdge>> adjacencyList = buildAdjacencyList(edges);
        
        // 对每个未掌握的单词，找到从最近已掌握单词出发的最优路径
        Set<Long> visited = new HashSet<>(masteredWordIds);
        PriorityQueue<PathSearchState> pq = new PriorityQueue<>(
                Comparator.comparingInt(s -> -s.totalWeight));
        
        // 初始化：从所有已掌握单词出发
        for (Long masteredId : masteredWordIds) {
            pq.offer(new PathSearchState(masteredId, 0, new ArrayList<>()));
        }
        
        Set<Long> reachedUnmastered = new HashSet<>();
        
        while (!pq.isEmpty() && reachedUnmastered.size() < unmasteredWordIds.size()) {
            PathSearchState current = pq.poll();
            
            if (unmasteredWordIds.contains(current.wordId) && 
                !reachedUnmastered.contains(current.wordId)) {
                recommendedPath.addAll(current.path);
                reachedUnmastered.add(current.wordId);
                visited.add(current.wordId);
                
                // 从这个新掌握的单词继续扩展
                pq.offer(new PathSearchState(current.wordId, 0, new ArrayList<>()));
                continue;
            }
            
            if (visited.contains(current.wordId) && !masteredWordIds.contains(current.wordId)) {
                continue;
            }
            visited.add(current.wordId);
            
            List<MindMapDTO.PathEdge> neighbors = adjacencyList.getOrDefault(current.wordId, Collections.emptyList());
            for (MindMapDTO.PathEdge edge : neighbors) {
                Long nextWordId = edge.getSource().equals(current.wordId) ? edge.getTarget() : edge.getSource();
                if (!visited.contains(nextWordId)) {
                    List<MindMapDTO.PathEdge> newPath = new ArrayList<>(current.path);
                    newPath.add(edge);
                    pq.offer(new PathSearchState(nextWordId, current.totalWeight + edge.getWeight(), newPath));
                }
            }
        }
        
        return recommendedPath;
    }
    
    private Map<Long, List<MindMapDTO.PathEdge>> buildAdjacencyList(List<MindMapDTO.PathEdge> edges) {
        Map<Long, List<MindMapDTO.PathEdge>> adjacencyList = new HashMap<>();
        for (MindMapDTO.PathEdge edge : edges) {
            adjacencyList.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge);
            adjacencyList.computeIfAbsent(edge.getTarget(), k -> new ArrayList<>()).add(edge);
        }
        return adjacencyList;
    }
    
    private static class PathSearchState {
        Long wordId;
        int totalWeight;
        List<MindMapDTO.PathEdge> path;
        
        PathSearchState(Long wordId, int totalWeight, List<MindMapDTO.PathEdge> path) {
            this.wordId = wordId;
            this.totalWeight = totalWeight;
            this.path = path;
        }
    }
    
    private MindMapDTO.WordNode createNode(Word word, int depth) {
        return MindMapDTO.WordNode.builder()
                .id(word.getId())
                .word(word.getWord())
                .meaning(word.getMeaning())
                .category(word.getPos())
                .depth(depth)
                .build();
    }
    
    private MindMapDTO.RelationEdge createEdge(WordRelation relation) {
        String label = getRelationLabel(relation.getRelationType());
        return MindMapDTO.RelationEdge.builder()
                .source(relation.getSourceWordId())
                .target(relation.getTargetWordId())
                .relationType(relation.getRelationType().name())
                .label(label)
                .build();
    }
    
    private String getRelationLabel(WordRelation.RelationType type) {
        switch (type) {
            case SYNONYM: return "同义";
            case ANTONYM: return "反义";
            case TOPIC: return "主题";
            case ROOT: return "词根";
            case PREFIX: return "前缀";
            case SUFFIX: return "后缀";
            case SCENE: return "场景";
            default: return type.name();
        }
    }
}
