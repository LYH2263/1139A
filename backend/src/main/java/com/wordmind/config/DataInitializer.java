package com.wordmind.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.wordmind.entity.*;
import com.wordmind.repository.*;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(UserRepository userRepo, WordRepository wordRepo, 
                               WordRelationRepository relationRepo, 
                               ReviewRecordRepository reviewRepo,
                               QuizRecordRepository quizRepo,
                               WordBookRepository wordBookRepo,
                               WordBookWordRepository wordBookWordRepo,
                               UserWordBookRepository userWordBookRepo,
                               WordExampleRepository wordExampleRepo,
                               PasswordEncoder encoder) {
        return args -> {
            // 创建测试用户
            if (!userRepo.existsByUsername("admin")) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(encoder.encode("admin123"));
                admin.setRole(User.Role.ADMIN);
                userRepo.save(admin);
            }
            
            if (!userRepo.existsByUsername("user")) {
                User user = new User();
                user.setUsername("user");
                user.setPassword(encoder.encode("user123"));
                user.setRole(User.Role.USER);
                userRepo.save(user);
            }
            
            // 创建30个不同场景的测试单词数据
            if (wordRepo.count() == 0) {
                // 场景1: 情感类 (Emotions)
                Word happy = createWord(wordRepo, "happy", "/ˈhæpi/", "adjective", 
                    "快乐的，幸福的", "I am very happy today.", "hap-py 开心的");
                Word joy = createWord(wordRepo, "joy", "/dʒɔɪ/", "noun", 
                    "欢乐，喜悦", "The children shouted with joy.", "joy 快乐");
                Word sad = createWord(wordRepo, "sad", "/sæd/", "adjective", 
                    "悲伤的", "She felt sad about the news.", "sad 悲伤");
                Word excited = createWord(wordRepo, "excited", "/ɪkˈsaɪtɪd/", "adjective",
                    "兴奋的", "I'm excited about the trip.", "ex-ci-ted 兴奋的");
                Word angry = createWord(wordRepo, "angry", "/ˈæŋɡri/", "adjective",
                    "生气的", "Don't be angry with me.", "an-gry 生气的");
                
                // 场景2: 食物类 (Food)
                Word apple = createWord(wordRepo, "apple", "/ˈæpl/", "noun", 
                    "苹果", "I eat an apple every day.", "a-pple 一个苹果");
                Word fruit = createWord(wordRepo, "fruit", "/fruːt/", "noun", 
                    "水果", "Apples are my favorite fruit.", "fruit 水果");
                Word bread = createWord(wordRepo, "bread", "/bred/", "noun",
                    "面包", "I had bread for breakfast.", "bread 面包");
                Word water = createWord(wordRepo, "water", "/ˈwɔːtər/", "noun",
                    "水", "Please drink more water.", "wa-ter 水");
                Word coffee = createWord(wordRepo, "coffee", "/ˈkɒfi/", "noun",
                    "咖啡", "I need a cup of coffee.", "cof-fee 咖啡");
                
                // 场景3: 动作类 (Actions)
                Word run = createWord(wordRepo, "run", "/rʌn/", "verb", 
                    "跑，奔跑", "I run every morning.", "run 跑");
                Word fast = createWord(wordRepo, "fast", "/fɑːst/", "adjective", 
                    "快的", "He is a fast runner.", "fast 快");
                Word walk = createWord(wordRepo, "walk", "/wɔːk/", "verb",
                    "走，步行", "Let's walk to school.", "walk 走");
                Word eat = createWord(wordRepo, "eat", "/iːt/", "verb",
                    "吃", "I eat breakfast at 7am.", "eat 吃");
                Word drink = createWord(wordRepo, "drink", "/drɪŋk/", "verb",
                    "喝", "I drink milk every day.", "drink 喝");
                
                // 场景4: 外貌类 (Appearance)
                Word beautiful = createWord(wordRepo, "beautiful", "/ˈbjuːtɪfl/", "adjective", 
                    "美丽的", "She is a beautiful girl.", "beau-ti-ful 美丽的");
                Word pretty = createWord(wordRepo, "pretty", "/ˈprɪti/", "adjective", 
                    "漂亮的", "You look pretty today.", "pret-ty 漂亮的");
                Word tall = createWord(wordRepo, "tall", "/tɔːl/", "adjective",
                    "高的", "He is very tall.", "tall 高的");
                Word short_word = createWord(wordRepo, "short", "/ʃɔːt/", "adjective",
                    "矮的，短的", "The pencil is short.", "short 短的");
                
                // 场景5: 大小类 (Size)
                Word big = createWord(wordRepo, "big", "/bɪɡ/", "adjective", 
                    "大的", "This is a big house.", "big 大");
                Word small = createWord(wordRepo, "small", "/smɔːl/", "adjective", 
                    "小的", "It's a small world.", "small 小");
                Word large = createWord(wordRepo, "large", "/lɑːdʒ/", "adjective",
                    "大的", "This is a large room.", "large 大的");
                Word tiny = createWord(wordRepo, "tiny", "/ˈtaɪni/", "adjective",
                    "微小的", "A tiny ant.", "ti-ny 微小的");
                
                // 场景6: 学习类 (Study)
                Word book = createWord(wordRepo, "book", "/bʊk/", "noun", 
                    "书", "I like reading books.", "book 书");
                Word read = createWord(wordRepo, "read", "/riːd/", "verb", 
                    "阅读", "I read books every night.", "read 读");
                Word write = createWord(wordRepo, "write", "/raɪt/", "verb",
                    "写", "Please write your name.", "write 写");
                Word study = createWord(wordRepo, "study", "/ˈstʌdi/", "verb",
                    "学习", "I study English every day.", "stu-dy 学习");
                
                // 场景7: 品质类 (Quality)
                Word good = createWord(wordRepo, "good", "/ɡʊd/", "adjective", 
                    "好的", "This is a good idea.", "good 好");
                Word bad = createWord(wordRepo, "bad", "/bæd/", "adjective", 
                    "坏的", "That's a bad habit.", "bad 坏");
                Word excellent = createWord(wordRepo, "excellent", "/ˈeksələnt/", "adjective",
                    "优秀的", "You did an excellent job!", "ex-cel-lent 优秀的");
                Word terrible = createWord(wordRepo, "terrible", "/ˈterəbl/", "adjective",
                    "糟糕的", "The weather is terrible.", "ter-ri-ble 糟糕的");
                
                // 场景8: 时间类 (Time)
                Word morning = createWord(wordRepo, "morning", "/ˈmɔːnɪŋ/", "noun",
                    "早晨", "Good morning!", "mor-ning 早晨");
                Word night = createWord(wordRepo, "night", "/naɪt/", "noun",
                    "夜晚", "Good night!", "night 夜晚");
                
                // 创建丰富的关系网络
                // 情感类关系
                createRelation(relationRepo, happy.getId(), joy.getId(), WordRelation.RelationType.SYNONYM);
                createRelation(relationRepo, happy.getId(), sad.getId(), WordRelation.RelationType.ANTONYM);
                createRelation(relationRepo, excited.getId(), happy.getId(), WordRelation.RelationType.SYNONYM);
                createRelation(relationRepo, angry.getId(), sad.getId(), WordRelation.RelationType.TOPIC);
                
                // 食物类关系
                createRelation(relationRepo, apple.getId(), fruit.getId(), WordRelation.RelationType.TOPIC);
                createRelation(relationRepo, bread.getId(), eat.getId(), WordRelation.RelationType.SCENE);
                createRelation(relationRepo, water.getId(), drink.getId(), WordRelation.RelationType.SCENE);
                createRelation(relationRepo, coffee.getId(), drink.getId(), WordRelation.RelationType.SCENE);
                
                // 动作类关系
                createRelation(relationRepo, run.getId(), fast.getId(), WordRelation.RelationType.TOPIC);
                createRelation(relationRepo, walk.getId(), run.getId(), WordRelation.RelationType.SYNONYM);
                createRelation(relationRepo, eat.getId(), drink.getId(), WordRelation.RelationType.TOPIC);
                
                // 外貌类关系
                createRelation(relationRepo, beautiful.getId(), pretty.getId(), WordRelation.RelationType.SYNONYM);
                createRelation(relationRepo, tall.getId(), short_word.getId(), WordRelation.RelationType.ANTONYM);
                
                // 大小类关系
                createRelation(relationRepo, big.getId(), small.getId(), WordRelation.RelationType.ANTONYM);
                createRelation(relationRepo, large.getId(), big.getId(), WordRelation.RelationType.SYNONYM);
                createRelation(relationRepo, tiny.getId(), small.getId(), WordRelation.RelationType.SYNONYM);
                
                // 学习类关系
                createRelation(relationRepo, book.getId(), read.getId(), WordRelation.RelationType.SCENE);
                createRelation(relationRepo, write.getId(), read.getId(), WordRelation.RelationType.TOPIC);
                createRelation(relationRepo, study.getId(), read.getId(), WordRelation.RelationType.TOPIC);
                
                // 品质类关系
                createRelation(relationRepo, good.getId(), bad.getId(), WordRelation.RelationType.ANTONYM);
                createRelation(relationRepo, excellent.getId(), good.getId(), WordRelation.RelationType.SYNONYM);
                createRelation(relationRepo, terrible.getId(), bad.getId(), WordRelation.RelationType.SYNONYM);
                
                // 时间类关系
                createRelation(relationRepo, morning.getId(), night.getId(), WordRelation.RelationType.ANTONYM);

                // 初始化语境例句数据
                if (wordExampleRepo.count() == 0) {
                    // happy (情感类)
                    createWordExample(wordExampleRepo, happy.getId(), "She was happy to see her old friends again.", "她很高兴再次见到她的老朋友。", "社交场景");
                    createWordExample(wordExampleRepo, happy.getId(), "The children were happy playing in the park.", "孩子们在公园里玩得很开心。", "休闲场景");
                    createWordExample(wordExampleRepo, happy.getId(), "I'm happy that you passed the exam.", "我很高兴你通过了考试。", "学业场景");

                    // sad (情感类)
                    createWordExample(wordExampleRepo, sad.getId(), "He felt sad when his dog passed away.", "当他的狗去世时，他感到很悲伤。", "情感场景");
                    createWordExample(wordExampleRepo, sad.getId(), "The sad movie made everyone cry.", "这部悲伤的电影让每个人都哭了。", "娱乐场景");
                    createWordExample(wordExampleRepo, sad.getId(), "She was sad about leaving her hometown.", "她对离开家乡感到难过。", "生活场景");

                    // excited (情感类)
                    createWordExample(wordExampleRepo, excited.getId(), "I'm so excited about the trip to Paris!", "我对去巴黎旅行感到非常兴奋！", "旅行场景");
                    createWordExample(wordExampleRepo, excited.getId(), "The children were excited to open their presents.", "孩子们兴奋地打开礼物。", "节日场景");
                    createWordExample(wordExampleRepo, excited.getId(), "She was excited to start her new job.", "她很兴奋开始她的新工作。", "职场场景");

                    // apple (食物类)
                    createWordExample(wordExampleRepo, apple.getId(), "An apple a day keeps the doctor away.", "每天一个苹果，医生远离我。", "健康场景");
                    createWordExample(wordExampleRepo, apple.getId(), "She picked a red apple from the tree.", "她从树上摘了一个红苹果。", "自然场景");
                    createWordExample(wordExampleRepo, apple.getId(), "Would you like some apple pie for dessert?", "你想要一些苹果派当甜点吗？", "饮食场景");

                    // water (食物类)
                    createWordExample(wordExampleRepo, water.getId(), "Please drink more water every day.", "请每天多喝水。", "健康场景");
                    createWordExample(wordExampleRepo, water.getId(), "The water in the lake is very clean.", "湖里的水非常清澈。", "自然场景");
                    createWordExample(wordExampleRepo, water.getId(), "Can I have a glass of water, please?", "请给我一杯水好吗？", "日常场景");

                    // run (动作类)
                    createWordExample(wordExampleRepo, run.getId(), "I run five kilometers every morning.", "我每天早上跑五公里。", "运动场景");
                    createWordExample(wordExampleRepo, run.getId(), "Don't run in the classroom.", "不要在教室里跑。", "学校场景");
                    createWordExample(wordExampleRepo, run.getId(), "She had to run to catch the bus.", "她不得不跑去赶公交车。", "交通场景");

                    // fast (大小/速度类)
                    createWordExample(wordExampleRepo, fast.getId(), "He is a very fast runner.", "他是一个跑得很快的人。", "运动场景");
                    createWordExample(wordExampleRepo, fast.getId(), "The fast train arrived on time.", "快速列车准时到达。", "交通场景");
                    createWordExample(wordExampleRepo, fast.getId(), "Don't drive too fast on the highway.", "在高速公路上不要开得太快。", "驾驶场景");

                    // beautiful (外貌类)
                    createWordExample(wordExampleRepo, beautiful.getId(), "What a beautiful sunset!", "多么美丽的日落啊！", "自然场景");
                    createWordExample(wordExampleRepo, beautiful.getId(), "She is a beautiful and kind person.", "她是一个美丽善良的人。", "人物描述");
                    createWordExample(wordExampleRepo, beautiful.getId(), "They live in a beautiful house by the sea.", "他们住在海边一栋漂亮的房子里。", "居住场景");

                    // big (大小类)
                    createWordExample(wordExampleRepo, big.getId(), "This is a big decision for our family.", "这对我们家来说是一个重大决定。", "生活场景");
                    createWordExample(wordExampleRepo, big.getId(), "He gave me a big surprise yesterday.", "他昨天给了我一个大惊喜。", "情感场景");
                    createWordExample(wordExampleRepo, big.getId(), "The company has a big office in Shanghai.", "该公司在上海有一个很大的办公室。", "职场场景");

                    // small (大小类)
                    createWordExample(wordExampleRepo, small.getId(), "She lives in a small apartment in the city.", "她住在城里的一套小公寓里。", "居住场景");
                    createWordExample(wordExampleRepo, small.getId(), "Even small changes can make a difference.", "即使是微小的改变也能带来不同。", "励志场景");
                    createWordExample(wordExampleRepo, small.getId(), "The small child was holding her mother's hand.", "那个小孩牵着她妈妈的手。", "家庭场景");

                    // book (学习类)
                    createWordExample(wordExampleRepo, book.getId(), "I'm reading an interesting book about history.", "我正在读一本关于历史的有趣的书。", "阅读场景");
                    createWordExample(wordExampleRepo, book.getId(), "Please book a table for two at the restaurant.", "请在餐厅预订一张两人桌。", "预订场景");
                    createWordExample(wordExampleRepo, book.getId(), "She wrote a book about her travels.", "她写了一本关于她旅行的书。", "创作场景");

                    // study (学习类)
                    createWordExample(wordExampleRepo, study.getId(), "I study English every evening after work.", "我每天晚上下班后学习英语。", "学习场景");
                    createWordExample(wordExampleRepo, study.getId(), "She went to the library to study for the exam.", "她去图书馆为考试学习。", "学业场景");
                    createWordExample(wordExampleRepo, study.getId(), "Scientists study the behavior of animals.", "科学家研究动物的行为。", "科研场景");

                    // good (品质类)
                    createWordExample(wordExampleRepo, good.getId(), "This is a very good restaurant.", "这是一家非常好的餐厅。", "餐饮场景");
                    createWordExample(wordExampleRepo, good.getId(), "He is a good friend who always helps me.", "他是一个总是帮助我的好朋友。", "社交场景");
                    createWordExample(wordExampleRepo, good.getId(), "Doing exercise is good for your health.", "锻炼对你的健康有益。", "健康场景");

                    // bad (品质类)
                    createWordExample(wordExampleRepo, bad.getId(), "Smoking is bad for your health.", "吸烟对你的健康有害。", "健康场景");
                    createWordExample(wordExampleRepo, bad.getId(), "He had a bad day at work today.", "他今天工作很不顺心。", "职场场景");
                    createWordExample(wordExampleRepo, bad.getId(), "The food at that restaurant was really bad.", "那家餐厅的食物真的很差。", "餐饮场景");

                    // morning (时间类)
                    createWordExample(wordExampleRepo, morning.getId(), "Good morning! How did you sleep?", "早上好！你睡得怎么样？", "问候场景");
                    createWordExample(wordExampleRepo, morning.getId(), "The morning sun is very warm and gentle.", "早晨的阳光温暖而柔和。", "自然场景");
                    createWordExample(wordExampleRepo, morning.getId(), "She drinks coffee every morning before work.", "她每天早上上班前喝咖啡。", "日常场景");

                    // night (时间类)
                    createWordExample(wordExampleRepo, night.getId(), "Good night! Sweet dreams!", "晚安！做个好梦！", "问候场景");
                    createWordExample(wordExampleRepo, night.getId(), "The night sky is full of stars tonight.", "今晚的夜空布满星星。", "自然场景");
                    createWordExample(wordExampleRepo, night.getId(), "He works the night shift at the hospital.", "他在医院上夜班。", "职场场景");
                }
            }

            // 初始化词书数据
            if (wordBookRepo.count() == 0) {
                // 创建初级词书：基础情感词汇
                WordBook emotionBook = new WordBook();
                emotionBook.setName("基础情感词汇");
                emotionBook.setDescription("学习表达喜怒哀乐等基本情感的英语词汇，适合英语初学者。");
                emotionBook.setDifficultyLevel(WordBook.DifficultyLevel.BEGINNER);
                emotionBook.setWordCount(5);
                wordBookRepo.save(emotionBook);

                // 创建中级词书：日常生活词汇
                WordBook dailyBook = new WordBook();
                dailyBook.setName("日常生活词汇");
                dailyBook.setDescription("涵盖饮食、运动、学习等日常生活场景的实用词汇。");
                dailyBook.setDifficultyLevel(WordBook.DifficultyLevel.INTERMEDIATE);
                dailyBook.setWordCount(10);
                wordBookRepo.save(dailyBook);

                // 创建高级词书：品质与描述
                WordBook advancedBook = new WordBook();
                advancedBook.setName("品质描述词汇");
                advancedBook.setDescription("用于描述事物品质、大小、外观的高级形容词，丰富你的表达能力。");
                advancedBook.setDifficultyLevel(WordBook.DifficultyLevel.ADVANCED);
                advancedBook.setWordCount(8);
                wordBookRepo.save(advancedBook);

                // 为情感词书添加单词 (ID 1-5: happy, joy, sad, excited, angry)
                for (long i = 1; i <= 5; i++) {
                    WordBookWord wbw = new WordBookWord();
                    wbw.setWordBookId(emotionBook.getId());
                    wbw.setWordId(i);
                    wordBookWordRepo.save(wbw);
                }

                // 为日常生活词书添加单词 (ID 6-15: apple, fruit, bread, water, coffee, run, fast, walk, eat, drink)
                for (long i = 6; i <= 15; i++) {
                    WordBookWord wbw = new WordBookWord();
                    wbw.setWordBookId(dailyBook.getId());
                    wbw.setWordId(i);
                    wordBookWordRepo.save(wbw);
                }

                // 为品质描述词书添加单词 (ID 16-23: beautiful, pretty, tall, short, big, small, large, tiny)
                for (long i = 16; i <= 23; i++) {
                    WordBookWord wbw = new WordBookWord();
                    wbw.setWordBookId(advancedBook.getId());
                    wbw.setWordId(i);
                    wordBookWordRepo.save(wbw);
                }

                // 为用户 user (ID=2) 默认添加情感词书到学习计划
                UserWordBook uwb = new UserWordBook();
                uwb.setUserId(2L);
                uwb.setWordBookId(emotionBook.getId());
                uwb.setMasteredCount(3);
                userWordBookRepo.save(uwb);
            }
            
            // 为用户 user (ID=2) 创建复习记录和测验记录（独立于单词初始化）
            User user = userRepo.findByUsername("user").orElse(null);
            if (user != null && reviewRepo.count() == 0) {
                createSampleReviewRecords(reviewRepo, user.getId());
                createSampleQuizRecords(quizRepo, user.getId());
            }
        };
    }
    
    private Word createWord(WordRepository repo, String word, String phonetic, String pos, 
                           String meaning, String example, String memoryTip) {
        Word w = new Word();
        w.setWord(word);
        w.setPhonetic(phonetic);
        w.setPos(pos);
        w.setMeaning(meaning);
        w.setExample(example);
        w.setMemoryTip(memoryTip);
        return repo.save(w);
    }
    
    private void createRelation(WordRelationRepository repo, Long source, Long target, WordRelation.RelationType type) {
        WordRelation r = new WordRelation();
        r.setSourceWordId(source);
        r.setTargetWordId(target);
        r.setRelationType(type);
        repo.save(r);
    }

    private void createWordExample(WordExampleRepository repo, Long wordId, String sentence, String translation, String scene) {
        WordExample e = new WordExample();
        e.setWordId(wordId);
        e.setSentence(sentence);
        e.setTranslation(translation);
        e.setScene(scene);
        repo.save(e);
    }
    
    private void createSampleReviewRecords(ReviewRecordRepository repo, Long userId) {
        // 创建一些已复习的记录（用于展示统计数据）
        LocalDateTime now = LocalDateTime.now();
        
        // 已掌握的单词（复习结果为 KNOWN）
        ReviewRecord r1 = new ReviewRecord();
        r1.setUserId(userId);
        r1.setWordId(1L); // happy
        r1.setResult(ReviewRecord.ReviewResult.KNOWN);
        r1.setProficiency(5);
        r1.setNextReviewAt(now.plusDays(7)); // 7天后复习
        r1.setCreatedAt(now.minusDays(1));
        repo.save(r1);
        
        ReviewRecord r2 = new ReviewRecord();
        r2.setUserId(userId);
        r2.setWordId(2L); // joy
        r2.setResult(ReviewRecord.ReviewResult.KNOWN);
        r2.setProficiency(5);
        r2.setNextReviewAt(now.plusDays(7));
        r2.setCreatedAt(now.minusDays(1));
        repo.save(r2);
        
        ReviewRecord r3 = new ReviewRecord();
        r3.setUserId(userId);
        r3.setWordId(3L); // sad
        r3.setResult(ReviewRecord.ReviewResult.KNOWN);
        r3.setProficiency(4);
        r3.setNextReviewAt(now.plusDays(3));
        r3.setCreatedAt(now.minusDays(1));
        repo.save(r3);
        
        // 模糊的单词（复习结果为 VAGUE）- 今天需要复习
        ReviewRecord r4 = new ReviewRecord();
        r4.setUserId(userId);
        r4.setWordId(4L); // excited
        r4.setResult(ReviewRecord.ReviewResult.VAGUE);
        r4.setProficiency(2);
        r4.setNextReviewAt(now.minusHours(2)); // 已经过期，需要复习
        r4.setCreatedAt(now.minusDays(2));
        repo.save(r4);
        
        ReviewRecord r5 = new ReviewRecord();
        r5.setUserId(userId);
        r5.setWordId(5L); // angry
        r5.setResult(ReviewRecord.ReviewResult.VAGUE);
        r5.setProficiency(2);
        r5.setNextReviewAt(now.minusHours(1)); // 已经过期，需要复习
        r5.setCreatedAt(now.minusDays(2));
        repo.save(r5);
        
        // 不认识的单词（复习结果为 UNKNOWN）- 今天需要复习
        ReviewRecord r6 = new ReviewRecord();
        r6.setUserId(userId);
        r6.setWordId(6L); // apple
        r6.setResult(ReviewRecord.ReviewResult.UNKNOWN);
        r6.setProficiency(1);
        r6.setNextReviewAt(now.minusHours(3)); // 已经过期，需要复习
        r6.setCreatedAt(now.minusDays(1));
        repo.save(r6);
        
        // 今天的复习记录
        ReviewRecord r7 = new ReviewRecord();
        r7.setUserId(userId);
        r7.setWordId(7L); // fruit
        r7.setResult(ReviewRecord.ReviewResult.KNOWN);
        r7.setProficiency(5);
        r7.setNextReviewAt(now.plusDays(7));
        r7.setCreatedAt(now.minusHours(1));
        repo.save(r7);
        
        ReviewRecord r8 = new ReviewRecord();
        r8.setUserId(userId);
        r8.setWordId(8L); // bread
        r8.setResult(ReviewRecord.ReviewResult.KNOWN);
        r8.setProficiency(4);
        r8.setNextReviewAt(now.plusDays(5));
        r8.setCreatedAt(now.minusMinutes(30));
        repo.save(r8);
    }
    
    private void createSampleQuizRecords(QuizRecordRepository repo, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        
        // 测验记录1：优秀
        QuizRecord q1 = new QuizRecord();
        q1.setUserId(userId);
        q1.setScore(90);
        q1.setCorrectCount(9);
        q1.setTotalCount(10);
        q1.setDuration(120);
        q1.setWrongWordIds("15"); // drink
        q1.setCreatedAt(now.minusDays(2));
        repo.save(q1);
        
        // 测验记录2：良好
        QuizRecord q2 = new QuizRecord();
        q2.setUserId(userId);
        q2.setScore(80);
        q2.setCorrectCount(8);
        q2.setTotalCount(10);
        q2.setDuration(150);
        q2.setWrongWordIds("12,14"); // fast, eat
        q2.setCreatedAt(now.minusDays(1));
        repo.save(q2);
        
        // 测验记录3：今天的测验
        QuizRecord q3 = new QuizRecord();
        q3.setUserId(userId);
        q3.setScore(100);
        q3.setCorrectCount(10);
        q3.setTotalCount(10);
        q3.setDuration(90);
        q3.setWrongWordIds("");
        q3.setCreatedAt(now.minusHours(2));
        repo.save(q3);
    }
}
