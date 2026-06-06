package com.wordmind.service;

import com.wordmind.dto.WordDTO;
import com.wordmind.entity.Word;
import com.wordmind.entity.WordExample;
import com.wordmind.entity.WordRelation;
import com.wordmind.repository.WordExampleRepository;
import com.wordmind.repository.WordRelationRepository;
import com.wordmind.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WordService {
    
    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private WordExampleRepository wordExampleRepository;

    @Autowired
    private WordRelationRepository wordRelationRepository;
    
    public WordDTO.ListResponse getWords(String keyword, String pos, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Word> wordPage = wordRepository.searchWords(keyword, pos, pageable);
        
        List<WordDTO.Response> list = wordPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return WordDTO.ListResponse.builder()
                .list(list)
                .total(wordPage.getTotalElements())
                .page(page)
                .size(size)
                .build();
    }
    
    public WordDTO.Response getWordById(Long id) {
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("单词不存在"));
        return convertToDetailDTO(word);
    }

    public List<WordDTO.ExampleResponse> getWordExamples(Long wordId) {
        if (!wordRepository.existsById(wordId)) {
            throw new RuntimeException("单词不存在");
        }
        List<WordExample> examples = wordExampleRepository.findByWordIdOrderByIdAsc(wordId);
        return examples.stream()
                .map(this::convertToExampleDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public WordDTO.Response createWord(WordDTO.CreateRequest request) {
        Word word = new Word();
        word.setWord(request.getWord());
        word.setPhonetic(request.getPhonetic());
        word.setPos(request.getPos());
        word.setMeaning(request.getMeaning());
        word.setExample(request.getExample());
        word.setMemoryTip(request.getMemoryTip());
        
        Word saved = wordRepository.save(word);
        return convertToDTO(saved);
    }
    
    @Transactional
    public WordDTO.Response updateWord(Long id, WordDTO.UpdateRequest request) {
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("单词不存在"));
        
        if (request.getWord() != null) word.setWord(request.getWord());
        if (request.getPhonetic() != null) word.setPhonetic(request.getPhonetic());
        if (request.getPos() != null) word.setPos(request.getPos());
        if (request.getMeaning() != null) word.setMeaning(request.getMeaning());
        if (request.getExample() != null) word.setExample(request.getExample());
        if (request.getMemoryTip() != null) word.setMemoryTip(request.getMemoryTip());
        
        Word saved = wordRepository.save(word);
        return convertToDTO(saved);
    }
    
    @Transactional
    public void deleteWord(Long id) {
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("单词不存在"));
        wordRepository.delete(word);
    }
    
    private WordDTO.Response convertToDTO(Word word) {
        return WordDTO.Response.builder()
                .id(word.getId())
                .word(word.getWord())
                .phonetic(word.getPhonetic())
                .pos(word.getPos())
                .meaning(word.getMeaning())
                .example(word.getExample())
                .memoryTip(word.getMemoryTip())
                .createdAt(word.getCreatedAt())
                .synonyms(new ArrayList<>())
                .antonyms(new ArrayList<>())
                .build();
    }

    private WordDTO.Response convertToDetailDTO(Word word) {
        WordDTO.Response dto = convertToDTO(word);
        List<WordRelation> relations = wordRelationRepository.findByWordId(word.getId());

        List<WordDTO.RelatedWord> synonyms = new ArrayList<>();
        List<WordDTO.RelatedWord> antonyms = new ArrayList<>();

        for (WordRelation relation : relations) {
            Long relatedWordId;
            if (relation.getSourceWordId().equals(word.getId())) {
                relatedWordId = relation.getTargetWordId();
            } else {
                relatedWordId = relation.getSourceWordId();
            }

            Word relatedWord = wordRepository.findById(relatedWordId).orElse(null);
            if (relatedWord == null) continue;

            WordDTO.RelatedWord related = WordDTO.RelatedWord.builder()
                    .id(relatedWord.getId())
                    .word(relatedWord.getWord())
                    .meaning(relatedWord.getMeaning())
                    .relationType(relation.getRelationType().name())
                    .build();

            if (relation.getRelationType() == WordRelation.RelationType.SYNONYM) {
                synonyms.add(related);
            } else if (relation.getRelationType() == WordRelation.RelationType.ANTONYM) {
                antonyms.add(related);
            }
        }

        dto.setSynonyms(synonyms);
        dto.setAntonyms(antonyms);
        return dto;
    }

    private WordDTO.ExampleResponse convertToExampleDTO(WordExample example) {
        return WordDTO.ExampleResponse.builder()
                .id(example.getId())
                .wordId(example.getWordId())
                .sentence(example.getSentence())
                .translation(example.getTranslation())
                .scene(example.getScene())
                .createdAt(example.getCreatedAt())
                .build();
    }
}
