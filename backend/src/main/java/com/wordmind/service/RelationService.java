package com.wordmind.service;

import com.wordmind.dto.MindMapDTO;
import com.wordmind.entity.User;
import com.wordmind.entity.Word;
import com.wordmind.entity.WordRelation;
import com.wordmind.repository.UserRepository;
import com.wordmind.repository.WordRelationRepository;
import com.wordmind.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RelationService {
    
    @Autowired
    private WordRelationRepository relationRepository;
    
    @Autowired
    private WordRepository wordRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Transactional
    public MindMapDTO.RelationEdge createRelation(MindMapDTO.RelationRequest request) {
        Word sourceWord = wordRepository.findById(request.getSourceWordId())
                .orElseThrow(() -> new RuntimeException("源单词不存在"));
        
        Word targetWord = wordRepository.findById(request.getTargetWordId())
                .orElseThrow(() -> new RuntimeException("目标单词不存在"));
        
        WordRelation.RelationType relationType;
        try {
            relationType = WordRelation.RelationType.valueOf(request.getRelationType());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("无效的关系类型");
        }
        
        boolean exists = relationRepository.existsBySourceWordIdAndTargetWordIdAndRelationType(
                request.getSourceWordId(), request.getTargetWordId(), relationType);
        if (exists) {
            throw new RuntimeException("该关系已存在");
        }
        
        WordRelation relation = new WordRelation();
        relation.setSourceWordId(request.getSourceWordId());
        relation.setTargetWordId(request.getTargetWordId());
        relation.setRelationType(relationType);
        
        WordRelation saved = relationRepository.save(relation);
        
        return buildRelationEdge(saved);
    }
    
    @Transactional
    public MindMapDTO.RelationEdge createRelationByUser(MindMapDTO.RelationRequest request, Long userId) {
        Word sourceWord = wordRepository.findById(request.getSourceWordId())
                .orElseThrow(() -> new RuntimeException("源单词不存在"));
        
        Word targetWord = wordRepository.findById(request.getTargetWordId())
                .orElseThrow(() -> new RuntimeException("目标单词不存在"));
        
        WordRelation.RelationType relationType;
        try {
            relationType = WordRelation.RelationType.valueOf(request.getRelationType());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("无效的关系类型");
        }
        
        boolean exists = relationRepository.existsBySourceWordIdAndTargetWordIdAndRelationType(
                request.getSourceWordId(), request.getTargetWordId(), relationType);
        if (exists) {
            throw new RuntimeException("该关系已存在");
        }
        
        WordRelation relation = new WordRelation();
        relation.setSourceWordId(request.getSourceWordId());
        relation.setTargetWordId(request.getTargetWordId());
        relation.setRelationType(relationType);
        relation.setCreatedBy(userId);
        relation.setStatus(WordRelation.RelationStatus.PENDING);
        
        WordRelation saved = relationRepository.save(relation);
        
        return buildRelationEdge(saved);
    }
    
    @Transactional
    public void deleteRelation(Long id) {
        WordRelation relation = relationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("关系不存在"));
        relationRepository.delete(relation);
    }
    
    @Transactional
    public void deleteRelationByUser(Long id, Long userId) {
        WordRelation relation = relationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("关系不存在"));
        
        if (relation.getCreatedBy() == null || !relation.getCreatedBy().equals(userId)) {
            throw new RuntimeException("只能删除自己创建的关系");
        }
        
        if (relation.getStatus() == WordRelation.RelationStatus.APPROVED) {
            throw new RuntimeException("已审核通过的关系无法删除，请联系管理员");
        }
        
        relationRepository.delete(relation);
    }
    
    public List<MindMapDTO.PendingRelationItem> getPendingRelations() {
        List<WordRelation> pendingRelations = relationRepository.findAllPending();
        
        List<Long> wordIds = pendingRelations.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getSourceWordId(), r.getTargetWordId()))
                .distinct()
                .collect(Collectors.toList());
        
        List<Long> userIds = pendingRelations.stream()
                .map(WordRelation::getCreatedBy)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, Word> wordMap = wordRepository.findAllById(wordIds).stream()
                .collect(Collectors.toMap(Word::getId, w -> w));
        
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        
        return pendingRelations.stream()
                .map(relation -> {
                    Word sourceWord = wordMap.get(relation.getSourceWordId());
                    Word targetWord = wordMap.get(relation.getTargetWordId());
                    User createdBy = relation.getCreatedBy() != null ? userMap.get(relation.getCreatedBy()) : null;
                    
                    return MindMapDTO.PendingRelationItem.builder()
                            .id(relation.getId())
                            .sourceWordId(relation.getSourceWordId())
                            .sourceWord(sourceWord != null ? sourceWord.getWord() : "未知")
                            .targetWordId(relation.getTargetWordId())
                            .targetWord(targetWord != null ? targetWord.getWord() : "未知")
                            .relationType(relation.getRelationType().name())
                            .relationLabel(getRelationLabel(relation.getRelationType()))
                            .createdBy(relation.getCreatedBy())
                            .createdByUsername(createdBy != null ? createdBy.getUsername() : "未知")
                            .createdAt(relation.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void reviewRelation(Long id, MindMapDTO.RelationReviewRequest request) {
        WordRelation relation = relationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("关系不存在"));
        
        if (relation.getStatus() != WordRelation.RelationStatus.PENDING) {
            throw new RuntimeException("该关系已被审核");
        }
        
        if (Boolean.TRUE.equals(request.getApproved())) {
            relation.setStatus(WordRelation.RelationStatus.APPROVED);
        } else {
            relation.setStatus(WordRelation.RelationStatus.REJECTED);
        }
        
        relationRepository.save(relation);
    }
    
    public MindMapDTO.RelationEdge buildRelationEdge(WordRelation relation) {
        User createdBy = relation.getCreatedBy() != null ? 
                userRepository.findById(relation.getCreatedBy()).orElse(null) : null;
        
        return MindMapDTO.RelationEdge.builder()
                .id(relation.getId())
                .source(relation.getSourceWordId())
                .target(relation.getTargetWordId())
                .relationType(relation.getRelationType().name())
                .label(getRelationLabel(relation.getRelationType()))
                .createdBy(relation.getCreatedBy())
                .createdByUsername(createdBy != null ? createdBy.getUsername() : null)
                .status(relation.getStatus() != null ? relation.getStatus().name() : null)
                .isUserContribution(relation.getCreatedBy() != null)
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
