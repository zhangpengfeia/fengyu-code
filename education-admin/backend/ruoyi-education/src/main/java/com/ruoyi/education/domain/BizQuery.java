package com.ruoyi.education.domain;
import java.util.*;

public class BizQuery {
    public final BizTable table;
    public Map<String,Object> filters = new LinkedHashMap<>();
    public List<Long> ids;
    public List<Long> schoolIds;
    public String keyword;
    public int limit = 20, offset = 0;
    public BizQuery(BizTable table) { this.table = table; }
    public BizQuery eq(String key,Object value) { if(value != null) filters.put(key,value); return this; }
}
