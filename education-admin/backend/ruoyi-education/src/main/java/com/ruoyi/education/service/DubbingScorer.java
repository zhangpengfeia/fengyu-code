package com.ruoyi.education.service;

/** Implement with the school's Baidu speech-evaluation product and credentials.
 * Implementations must return provider marks, never values from request JSON. */
public interface DubbingScorer {
    Result evaluate(String materialText,String audioUrl);
    record Result(int accuracy,int fluency,int completeness,int phonology,int intonation,int overall) {
        public int total(){return accuracy+fluency+completeness+phonology+intonation+overall;}
    }
}
