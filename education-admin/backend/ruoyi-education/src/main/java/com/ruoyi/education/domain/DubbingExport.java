package com.ruoyi.education.domain;
import com.ruoyi.common.annotation.Excel;
import java.util.*;
public class DubbingExport {
    @Excel(name="学校ID") private String school_id;
    public String getSchool_id(){return school_id;} public void setSchool_id(String v){school_id=v;}
    @Excel(name="班级ID") private String class_id;
    public String getClass_id(){return class_id;} public void setClass_id(String v){class_id=v;}
    @Excel(name="学生ID") private String student_id;
    public String getStudent_id(){return student_id;} public void setStudent_id(String v){student_id=v;}
    @Excel(name="册次") private String volume;
    public String getVolume(){return volume;} public void setVolume(String v){volume=v;}
    @Excel(name="素材标题") private String title;
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    @Excel(name="配音类型 1课件 2测评") private String dubbing_type;
    public String getDubbing_type(){return dubbing_type;} public void setDubbing_type(String v){dubbing_type=v;}
    @Excel(name="答题时间") private String answer_time;
    public String getAnswer_time(){return answer_time;} public void setAnswer_time(String v){answer_time=v;}
    @Excel(name="准确度") private String accuracy_score;
    public String getAccuracy_score(){return accuracy_score;} public void setAccuracy_score(String v){accuracy_score=v;}
    @Excel(name="流畅度") private String fluency_score;
    public String getFluency_score(){return fluency_score;} public void setFluency_score(String v){fluency_score=v;}
    @Excel(name="完整性") private String completeness_score;
    public String getCompleteness_score(){return completeness_score;} public void setCompleteness_score(String v){completeness_score=v;}
    @Excel(name="声韵") private String phonology_score;
    public String getPhonology_score(){return phonology_score;} public void setPhonology_score(String v){phonology_score=v;}
    @Excel(name="语调") private String intonation_score;
    public String getIntonation_score(){return intonation_score;} public void setIntonation_score(String v){intonation_score=v;}
    @Excel(name="整体印象") private String overall_score;
    public String getOverall_score(){return overall_score;} public void setOverall_score(String v){overall_score=v;}
    @Excel(name="总分") private String total_score;
    public String getTotal_score(){return total_score;} public void setTotal_score(String v){total_score=v;}
    @Excel(name="星数") private String star_num;
    public String getStar_num(){return star_num;} public void setStar_num(String v){star_num=v;}
    @Excel(name="录音地址") private String audio_url;
    public String getAudio_url(){return audio_url;} public void setAudio_url(String v){audio_url=v;}
    public DubbingExport(){}
    public DubbingExport(Map<String,Object> r){
        school_id=Objects.toString(r.get("school_id"),"");
        class_id=Objects.toString(r.get("class_id"),"");
        student_id=Objects.toString(r.get("student_id"),"");
        volume=Objects.toString(r.get("volume"),"");
        title=Objects.toString(r.get("title"),"");
        dubbing_type=Objects.toString(r.get("dubbing_type"),"");
        answer_time=Objects.toString(r.get("answer_time"),"");
        accuracy_score=Objects.toString(r.get("accuracy_score"),"");
        fluency_score=Objects.toString(r.get("fluency_score"),"");
        completeness_score=Objects.toString(r.get("completeness_score"),"");
        phonology_score=Objects.toString(r.get("phonology_score"),"");
        intonation_score=Objects.toString(r.get("intonation_score"),"");
        overall_score=Objects.toString(r.get("overall_score"),"");
        total_score=Objects.toString(r.get("total_score"),"");
        star_num=Objects.toString(r.get("star_num"),"");
        audio_url=Objects.toString(r.get("audio_url"),"");
    }
}
