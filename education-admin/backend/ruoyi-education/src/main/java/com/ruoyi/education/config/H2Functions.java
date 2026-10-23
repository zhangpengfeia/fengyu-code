package com.ruoyi.education.config;
/** MySQL FIND_IN_SET compatibility for the isolated H2 development profile only. */
public final class H2Functions {
    private H2Functions() {}
    public static java.sql.Timestamp sysdate() { return java.sql.Timestamp.from(java.time.Instant.now()); }
    public static int findInSet(String needle,String csv) {
        if(needle==null||csv==null)return 0;String[] values=csv.split(",");
        for(int i=0;i<values.length;i++)if(values[i].equals(needle))return i+1;return 0;
    }
}
