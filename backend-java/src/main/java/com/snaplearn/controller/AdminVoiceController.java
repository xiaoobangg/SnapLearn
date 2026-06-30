package com.snaplearn.controller;

import com.snaplearn.entity.Voice;
import com.snaplearn.service.VoiceService;
import lombok.extern.slf4j.Slf4j;
import com.snaplearn.service.tts.TtsService;
import com.snaplearn.service.tts.VoiceEnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin 音色管理 + 测试 + 声音复刻
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/voices")
@RequiredArgsConstructor
public class AdminVoiceController {

    private final VoiceService voiceService;
    private final TtsService ttsService;
    private final VoiceEnrollmentService enrollmentService;

    @GetMapping
    public List<Map<String, Object>> list() {
        return voiceService.listAll().stream().map(this::toMap).collect(Collectors.toList());
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Voice voice) {
        voiceService.create(voice);
        return toMap(voice);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id, @RequestBody Voice voice) {
        voice.setId(id);
        voiceService.update(voice);
        return toMap(voice);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        voiceService.delete(id);
        return Map.of("ok", true);
    }

    @PostMapping("/{id}/default")
    public Map<String, Object> setDefault(@PathVariable String id) {
        voiceService.setAsDefault(id);
        return Map.of("ok", true);
    }

    /** 官方音色库（cosyvoice-v3-plus），供 Admin 浏览勾选入库 */
    @GetMapping("/catalog")
    public List<Map<String, String>> catalog() {
        return List.of(
                // ===== 语音助手 / 通用（中文普通话 + 英文） =====
                Map.of("model","cosyvoice-v3-plus","name", "龙小淳（知性积极女）", "voice_code", "longxiaochun_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙小夏（沉稳权威女）", "voice_code", "longxiaoxia_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙安昀（居家暖男）", "voice_code", "longanyun_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙安温（优雅知性女）", "voice_code", "longanwen_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙安莉（利落从容女）", "voice_code", "longanli_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙安朗（清爽利落男）", "voice_code", "longanlang_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙应沐（优雅知性女）", "voice_code", "longyingmu_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "YUMI（正经青年女）", "voice_code", "longyumi_v3", "language", "中文/英文"),
                // ===== 社交陪伴 =====
                Map.of("model","cosyvoice-v3-plus","name", "龙安洋（阳光大男孩）", "voice_code", "longanyang", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙安欢V3（方言·元气女）", "voice_code", "longanhuan_v3", "language", "中文/英文/方言"),
                Map.of("model","cosyvoice-v3-plus","name", "龙安欢（元气女）", "voice_code", "longanhuan", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙安柔（温柔闺蜜女）", "voice_code", "longanrou_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙安智（睿智轻熟男）", "voice_code", "longanzhi_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙安灵（思维灵动女）", "voice_code", "longanling_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙安雅（高雅气质女）", "voice_code", "longanya_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙安亲（亲和活泼女）", "voice_code", "longanqin_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙安台（嗲甜台湾女）", "voice_code", "longantai_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙华（元气甜美女）", "voice_code", "longhua_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙橙（智慧青年男）", "voice_code", "longcheng_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙泽（温暖元气男）", "voice_code", "longze_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙哲（呆板大暖男）", "voice_code", "longzhe_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙颜（温暖春风女）", "voice_code", "longyan_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙星（温婉邻家女）", "voice_code", "longxing_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙天（磁性理智男）", "voice_code", "longtian_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙婉（细腻柔声女）", "voice_code", "longwan_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙菲菲（甜美娇气女）", "voice_code", "longfeifei_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙浩（多情忧郁男）", "voice_code", "longhao_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙寒（温暖痴情男）", "voice_code", "longhan_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙嫱（浪漫风情女）", "voice_code", "longqiang_v3", "language", "中文/英文"),
                // ===== 童声 =====
                Map.of("model","cosyvoice-v3-plus","name", "龙呼呼（天真烂漫女童）", "voice_code", "longhuhu_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙泡泡（飞天泡泡音）", "voice_code", "longpaopao_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙杰力豆（阳光顽皮男）", "voice_code", "longjielidou_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙仙（豪放可爱女）", "voice_code", "longxian_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙铃（稚气呆板女）", "voice_code", "longling_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙闪闪（戏剧化童声）", "voice_code", "longshanshan_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙牛牛（阳光男童声）", "voice_code", "longniuniu_v3", "language", "中文/英文"),
                // ===== 英文（美式/英式） =====
                Map.of("model","cosyvoice-v3-plus","name", "loongabby（美式英文女）", "voice_code", "loongabby_v3", "language", "美式英语"),
                Map.of("model","cosyvoice-v3-plus","name", "loongandy（美式英文男）", "voice_code", "loongandy_v3", "language", "美式英语"),
                Map.of("model","cosyvoice-v3-plus","name", "loongannie（美式英文女）", "voice_code", "loongannie_v3", "language", "美式英语"),
                Map.of("model","cosyvoice-v3-plus","name", "loongava（美式英文女）", "voice_code", "loongava_v3", "language", "美式英语"),
                Map.of("model","cosyvoice-v3-plus","name", "loongbeth（美式英文女）", "voice_code", "loongbeth_v3", "language", "美式英语"),
                Map.of("model","cosyvoice-v3-plus","name", "loongcally（美式英文女）", "voice_code", "loongcally_v3", "language", "美式英语"),
                Map.of("model","cosyvoice-v3-plus","name", "loongcindy（美式英文女）", "voice_code", "loongcindy_v3", "language", "美式英语"),
                Map.of("model","cosyvoice-v3-plus","name", "loongdavid（美式英文男）", "voice_code", "loongdavid_v3", "language", "美式英语"),
                Map.of("model","cosyvoice-v3-plus","name", "loongdonna（美式英文女）", "voice_code", "loongdonna_v3", "language", "美式英语"),
                Map.of("model","cosyvoice-v3-plus","name", "loongbetty（美式英文女）", "voice_code", "loongbetty_v3", "language", "美式英语"),
                Map.of("model","cosyvoice-v3-plus","name", "loongemily（英式英文女）", "voice_code", "loongemily_v3", "language", "英式英语"),
                Map.of("model","cosyvoice-v3-plus","name", "loongeric（英式英文男）", "voice_code", "loongeric_v3", "language", "英式英语"),
                Map.of("model","cosyvoice-v3-plus","name", "loongluna（英式英文女）", "voice_code", "loongluna_v3", "language", "英式英语"),
                Map.of("model","cosyvoice-v3-plus","name", "loongluca（英式英文男）", "voice_code", "loongluca_v3", "language", "英式英语"),
                // ===== 有声书 =====
                Map.of("model","cosyvoice-v3-plus","name", "龙妙（抑扬顿挫女）", "voice_code", "longmiao_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙三叔（沉稳质感男）", "voice_code", "longsanshu_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙媛（温暖治愈女）", "voice_code", "longyuan_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙悦（温暖磁性女）", "voice_code", "longyue_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙修（博才说书男）", "voice_code", "longxiu_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙楠（睿智青年男）", "voice_code", "longnan_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙老伯（沧桑岁月爷）", "voice_code", "longlaobo_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙老姨（烟火从容阿姨）", "voice_code", "longlaoyi_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙婉君（细腻柔声女）", "voice_code", "longwanjun_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙逸尘（洒脱活力男）", "voice_code", "longyichen_v3", "language", "中文/英文"),
                // ===== 客服 =====
                Map.of("model","cosyvoice-v3-plus","name", "龙应询（年轻青涩男）", "voice_code", "longyingxun_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙应静（低调冷静女）", "voice_code", "longyingjing_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙应聆（温和共情女）", "voice_code", "longyingling_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙应桃（温柔淡定女）", "voice_code", "longyingtao_v3", "language", "中文/英文"),
                // ===== 新闻播报 =====
                Map.of("model","cosyvoice-v3-plus","name", "龙硕（博才干练男）", "voice_code", "longshuo_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙书（沉稳青年男）", "voice_code", "longshu_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "Bella3.0（精准干练女）", "voice_code", "loongbella_v3", "language", "中文/英文"),
                // ===== 直播/电话销售 =====
                Map.of("model","cosyvoice-v3-plus","name", "龙安燃（活泼质感女）", "voice_code", "longanran_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙安宣（经典直播女）", "voice_code", "longanxuan_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙应笑（清甜推销女）", "voice_code", "longyingxiao_v3", "language", "中文/英文"),
                // ===== 方言 =====
                Map.of("model","cosyvoice-v3-plus","name", "龙嘉欣（优雅粤语女）", "voice_code", "longjiaxin_v3", "language", "粤语/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙嘉怡（知性粤语女）", "voice_code", "longjiayi_v3", "language", "粤语/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙安粤（欢脱粤语男）", "voice_code", "longanyue_v3", "language", "粤语/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙老铁（东北直率男）", "voice_code", "longlaotie_v3", "language", "东北话/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙陕哥（原味陕北男）", "voice_code", "longshange_v3", "language", "陕西话/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙安闽（清纯闽南女）", "voice_code", "longanmin_v3", "language", "闽南话/英文"),
                // ===== 诗词 / 短视频配音 =====
                Map.of("model","cosyvoice-v3-plus","name", "龙飞（热血磁性男）", "voice_code", "longfei_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙机器（呆萌机器人）", "voice_code", "longjiqi_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙猴哥（经典猴哥）", "voice_code", "longhouge_v3", "language", "中文/英文"),
                Map.of("model","cosyvoice-v3-plus","name", "龙黛玉（娇率才女音）", "voice_code", "longdaiyu_v3", "language", "中文/英文"),
                // ===== 外语 =====
                Map.of("model","cosyvoice-v3-plus","name", "loongkyong（韩语女）", "voice_code", "loongkyong_v3", "language", "韩语"),
                Map.of("model","cosyvoice-v3-plus","name", "Riko（二次元霓虹女）", "voice_code", "loongriko_v3", "language", "日语"),
                Map.of("model","cosyvoice-v3-plus","name", "loongtomoka（日语女）", "voice_code", "loongtomoka_v3", "language", "日语"),
                Map.of("model","cosyvoice-v3-plus","name", "loongtomoya（日语男）", "voice_code", "loongtomoya_v3", "language", "日语"),
                Map.of("model","cosyvoice-v3-plus","name", "Yuuna（元气霓虹女）", "voice_code", "loongyuuna_v3", "language", "日语"),
                Map.of("model","cosyvoice-v3-plus","name", "Yuuma（干练霓虹男）", "voice_code", "loongyuuma_v3", "language", "日语"),
                Map.of("model","cosyvoice-v3-plus","name", "Jihun（阳光韩国男）", "voice_code", "loongjihun_v3", "language", "韩语"),
                Map.of("model","cosyvoice-v3-plus","name", "loongindah（印尼女）", "voice_code", "loongindah_v3", "language", "印尼语")
        );
    }

    /** 从官方音色库批量导入到用户音色库 */
    @PostMapping("/import")
    public Map<String, Object> importVoices(@RequestBody List<Map<String, String>> voices) {
        int count = 0;
        for (Map<String, String> v : voices) {
            Voice voice = new Voice();
            voice.setName(v.get("name"));
            voice.setProvider("dashscope");
            voice.setVoiceCode(v.get("voice_code"));
            voice.setTtsModel(v.getOrDefault("model", "cosyvoice-v3-plus"));
            voice.setDescription(v.get("language"));
            voiceService.create(voice);
            count++;
        }
        return Map.of("ok", true, "imported", count);
    }

    /** 测试合成：返回音频 URL */
    @PostMapping("/test/{id}")
    public Map<String, Object> test(@PathVariable String id, @RequestBody Map<String, String> body) {
        Voice v = voiceService.listAll().stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
        if (v == null) return Map.of("ok", false, "detail", "音色不存在");

        String text = body.getOrDefault("text", "你好，这是测试语音");
        try {
            String url = ttsService.synthesizeAndSave(v, text, "test-" + id.substring(0, 8));
            return Map.of("ok", true, "audio_url", url);
        } catch (Exception e) {
            return Map.of("ok", false, "detail", e.getMessage());
        }
    }

    private Map<String, Object> toMap(Voice v) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", v.getId());
        m.put("name", v.getName());
        m.put("provider", v.getProvider());
        m.put("voice_code", v.getVoiceCode());
        m.put("tts_model", v.getTtsModel() != null ? v.getTtsModel() : "cosyvoice-v3-plus");
        m.put("format", v.getFormat() != null ? v.getFormat() : "mp3");
        m.put("sample_rate", v.getSampleRate() != null ? v.getSampleRate() : 22050);
        m.put("volume", v.getVolume() != null ? v.getVolume() : 50);
        m.put("speech_rate", v.getSpeechRate() != null ? v.getSpeechRate() : 1.0);
        m.put("pitch", v.getPitch() != null ? v.getPitch() : 1.0);
        m.put("instruction", v.getInstruction() != null ? v.getInstruction() : "");
        m.put("description", v.getDescription() != null ? v.getDescription() : "");
        m.put("is_default", v.getIsDefault());
        m.put("is_active", v.getIsActive());
        return m;
    }

    // ===== 声音复刻 =====

    /** 文件上传模式：上线后使用，服务器公网域名拼 URL */
    @PostMapping(value = "/enroll", consumes = "multipart/form-data")
    public Map<String, Object> enrollFile(@RequestParam("file") MultipartFile file,
                                           @RequestParam("name") String name,
                                           @RequestParam(value = "model", defaultValue = "cosyvoice-v3-plus") String targetModel) {
        try {
            String voiceId = enrollmentService.enroll(file, name, targetModel);
            return saveVoice(name, voiceId, "声音复刻 · " + targetModel, targetModel);
        } catch (Exception e) {
            return Map.of("ok", false, "detail", e.getMessage());
        }
    }

    /** URL 模式：本地开发/已有公网文件 */
    @PostMapping(value = "/enroll", consumes = "application/json")
    public Map<String, Object> enrollUrl(@RequestBody Map<String, String> body) {
        try {
            String audioUrl = body.get("url");
            String name = body.get("name");
            String targetModel = body.getOrDefault("model", "cosyvoice-v3-plus");
            String voiceId = enrollmentService.enroll(audioUrl, name, targetModel);
            return saveVoice(name, voiceId, "声音复刻（URL）· " + targetModel, targetModel);
        } catch (Exception e) {
            return Map.of("ok", false, "detail", e.getMessage());
        }
    }

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    /** 查询阿里云上已复刻的音色列表 */
    @GetMapping("/enroll")
    public Object listEnrolled() {
        try {
            String raw = enrollmentService.listEnrolled();
            var root = objectMapper.readTree(raw);
            var list = root.path("output").path("voice_list");
            List<Map<String, Object>> items = new ArrayList<>();
            for (var v : list) {
                items.add(Map.of(
                        "voice_id", v.path("voice_id").asText(),
                        "target_model", v.path("target_model").asText(),
                        "status", v.path("status").asText(),
                        "gmt_create", v.path("gmt_create").asText()
                ));
            }
            return items;
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    /** 将云端复刻音色导入本地音色库 */
    @PostMapping("/enroll/import")
    public Map<String, Object> importEnrolled(@RequestBody Map<String, String> body) {
        try {
            String voiceId = body.get("voice_id");
            String targetModel = body.getOrDefault("target_model", "cosyvoice-v3-plus");
            Voice v = new Voice();
            v.setName(body.getOrDefault("name", "复刻音色"));
            v.setProvider("dashscope");
            v.setVoiceCode(voiceId);
            v.setTtsModel(targetModel);  // 复刻音色必须用创建时的 target_model
            v.setDescription("声音复刻 · " + targetModel);
            voiceService.create(v);
            return Map.of("ok", true);
        } catch (Exception e) {
            return Map.of("ok", false, "detail", e.getMessage());
        }
    }

    /** 删除复刻音色（同时删阿里云 + 本地 DB） */
    @DeleteMapping("/enroll/{voiceCode}")
    public Map<String, Object> deleteEnrolled(@PathVariable String voiceCode) {
        try {
            enrollmentService.delete(voiceCode);
        } catch (Exception e) {
            log.warn("[VOICE] cloud delete failed voiceCode={}", voiceCode, e);
            return Map.of("ok", false, "detail", e.getMessage());
        }
        return Map.of("ok", true);
    }

    private Map<String, Object> saveVoice(String name, String voiceId, String desc, String targetModel) {
        Voice v = new Voice();
        v.setName(name);
        v.setProvider("dashscope");
        v.setVoiceCode(voiceId);
        v.setTtsModel(targetModel != null ? targetModel : "cosyvoice-v3-plus");
        v.setDescription(desc);
        voiceService.create(v);
        return Map.of("ok", true, "voice_id", voiceId, "voice_code", voiceId);
    }
}
