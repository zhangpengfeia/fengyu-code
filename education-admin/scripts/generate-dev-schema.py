#!/usr/bin/env python3
"""Generate ephemeral H2 schema from reviewed MySQL migrations; never run against production."""
from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]
s='\n'.join((root/'sql'/n).read_text() for n in ['V001__ruoyi_baseline.sql','V002__education_schema.sql','V003__menus_and_catalog.sql'])
s=re.sub(r'\)\s*engine=innodb[^;]*;',');',s,flags=re.I)
s=re.sub(r'\b(bigint|int|tinyint|smallint)\(\d+\)',r'\1',s,flags=re.I)
s=re.sub(r"\bcomment\s*(?:=\s*)?'[^']*'",'',s,flags=re.I)
s=re.sub(r'\bunsigned\b','',s,flags=re.I)
s=re.sub(r'(?i)sysdate\(\)','CURRENT_TIMESTAMP',s)
s=s.replace("\\'", "''")
s="CREATE ALIAS IF NOT EXISTS FIND_IN_SET FOR 'com.ruoyi.education.config.H2Functions.findInSet';\n"+s
out=root/'backend/ruoyi-admin/src/main/resources/db/development.sql'
out.parent.mkdir(parents=True,exist_ok=True);out.write_text(s)
print('Generated development schema (no database connection made).')
