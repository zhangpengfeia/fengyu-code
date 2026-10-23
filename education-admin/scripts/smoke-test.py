#!/usr/bin/env python3
"""Destructive ONLY to disposable dev fixtures; never point this at a production instance."""
import argparse,json,os,re,subprocess,urllib.request,urllib.error
from datetime import datetime,timezone,timedelta
p=argparse.ArgumentParser();p.add_argument('--base',default='http://127.0.0.1:8081');p.add_argument('--redis-cli');p.add_argument('--redis-port',default='6379');p.add_argument('--allow-development',action='store_true');args=p.parse_args()
if not args.allow_development or not re.match(r'^http://(127\.0\.0\.1|localhost):\d+$',args.base):p.error('Only explicitly allowed loopback disposable development environments may be tested.')
admin_password=os.environ['EDU_TEST_ADMIN_PASSWORD'];demo_password=os.environ['EDU_TEST_DEMO_PASSWORD']
results=[]
class NoRedirect(urllib.request.HTTPRedirectHandler):
 def redirect_request(self,*a,**kw):return None
opener=urllib.request.build_opener(NoRedirect)
def call(path,method='GET',data=None,token=None,cookie=None):
 headers={'Content-Type':'application/json'}
 if token:headers['Authorization']='Bearer '+token
 if cookie:headers['Cookie']=cookie
 req=urllib.request.Request(args.base+path,data=json.dumps(data).encode() if data is not None else None,headers=headers,method=method)
 try:r=opener.open(req,timeout=20)
 except urllib.error.HTTPError as e:r=e
 raw=r.read();text=raw.decode(errors='replace')
 try:body=json.loads(text)
 except ValueError:body=text
 return r.code,body,r.headers

def check(label,condition,detail=''):
 if not condition:raise AssertionError(label+': '+str(detail))
 results.append(label);print('PASS '+label)
def login(username,password,mode='ACCOUNT',school=None):
 status,c,_=call('/captchaImage');code=''
 if c.get('captchaEnabled',True):
  if not args.redis_cli:raise RuntimeError('Memory-mode smoke tests require the disposable dev server started with --education.dev-captcha-enabled=false; normal dev keeps captcha enabled.')
  raw=subprocess.check_output([args.redis_cli,'-p',args.redis_port,'--raw','GET','captcha_codes:'+c['uuid']],text=True).strip()
  code=json.loads(raw)
 status,r,_=call('/biz/auth/login','POST',{'username':username,'password':password,'mode':mode,'school_id':school,'code':str(code),'uuid':c.get('uuid','')})
 check('login '+mode+' '+username,status==200 and r.get('code')==200,r)
 return r['token']
def launch(token,gid):
 status,r,_=call(f'/biz/games/{gid}/launch','POST',{},token);check('game launch '+str(gid),status==200,r)
 path=r['data']['url'].split(args.base)[-1]
 # The default game origin is loopback; extract path without forwarding credentials elsewhere.
 from urllib.parse import urlsplit
 parsed=urlsplit(r['data']['url']);check('launch origin is loopback',parsed.hostname in ['localhost','127.0.0.1'])
 path=parsed.path+'?'+parsed.query
 status,b,h=call(path);check('single-use ticket exchange',status==303,b);cookie=h.get('Set-Cookie').split(';')[0]
 status,b,_=call(path);check('ticket replay denied',status==403,b)
 return cookie
admin=login('admin',admin_password)
status,me,_=call('/biz/auth/me',token=admin)
if me['data']['must_change_pwd']:
 rotated='ValidationOnly!2026'
 status,r,_=call('/biz/auth/password','POST',{'old_password':admin_password,'new_password':rotated},admin);check('initial password changed',status==200,r)
 status,_,_=call('/biz/auth/me',token=admin);check('old bearer invalid after password change',status==401)
 admin=login('admin',rotated)
for attempt in range(2):
 status,r,_=call('/system/dict/data/type/sys_normal_disable',token=admin)
 check('native dictionary '+('cache read' if attempt else 'first load'),status==200 and r.get('code')==200 and len(r.get('data',[]))>=2,r)
teacher=login('teacher',demo_password);student=login('student',demo_password)
phone=login('13800000002',demo_password,'PHONE');school=login('0001',demo_password,'STUDENT','11000')
status,r,_=call('/getRouters',token=student);check('student menu excludes user management','用户管理' not in json.dumps(r,ensure_ascii=False))
status,r,_=call('/biz/catalog/games',token=student);check('student catalog excludes teacher-only game',status==200 and all(x['game_code']!='planet' for x in r['rows']),r)
status,r,_=call('/biz/games/1/launch','POST',{},student);check('student launch planet denied',status==403,r)
status,_,_=call('/play/planet/index.html');check('anonymous planet HTML denied',status==403)
status,_,_=call('/play/planet/assets/anything.js');check('anonymous game script denied',status==403)
cookie=launch(teacher,1)
status,html,_=call('/play/planet/index.html',cookie=cookie);check('teacher planet HTML allowed',status==200,html[:200])
asset=re.search(r'(?:src|href)="(/play/planet/assets/[^\"]+\.js)"',html).group(1)
status,_,_=call(asset,cookie=cookie);check('teacher hashed game script allowed',status==200)
status,_,_=call(asset);check('same hashed script without session denied',status==403)
status,_,_=call('/play/number-match/index.html',cookie=cookie);check('game session cannot cross game boundary',status==403)
student_cookie=launch(student,2)
status,_,_=call('/play/number-match/index.html',cookie=student_cookie);check('authorized student game works',status==200)
status,r,_=call('/biz/entitlements?student_user_id=20002',token=admin)
for e in r['data']:
 if e.get('remark')=='开发样例游戏授权':
  status,b,_=call('/biz/entitlements/'+str(e['entitlement_id'])+'/revoke','POST',{'reason':'端到端撤销验证'},admin);check('grant revoked',status==200,b)
status,_,_=call('/play/number-match/index.html',cookie=student_cookie);check('existing game session denied immediately after revocation',status==403)
status,_,_=call('/biz/users',token=student);check('student user-management API denied',status==403)
status,r,_=call('/biz/users?size=100',token=teacher);check('teacher only sees assigned students',status==200 and len(r['rows'])==1 and str(r['rows'][0]['user_id'])=='20002',r)
status,r,_=call('/biz/catalog/schools','POST',{'school_id':101,'school_code':'OTHER','school_name':'第二测试学校','expire_time':(datetime.now(timezone.utc)+timedelta(days=30)).isoformat().replace('+00:00','Z'),'school_status':'ACTIVE'},admin);check('second school fixture created',status==200,r)
manager=login('schooladmin',demo_password)
status,r,_=call('/biz/catalog/schools/101?edit=true',token=manager);check('cross-school details denied',status==403,r)
status,r,_=call('/biz/users/20002','PUT',{'school_id':11000,'role_key':'admin','nick_name':'越权'},manager);check('school administrator cannot elevate roles',status==403,r)
status,new,_=call('/biz/users','POST',{'school_id':11000,'role_key':'edu_teacher','user_name':'fresh_teacher','nick_name':'新教师','class_ids':[],'status':'0','phone':'13800000099'},admin)
check('new account gets generated initial password',status==200 and bool(new.get('data',{}).get('temporary_password')),new)
fresh=login(new['data']['user_name'],new['data']['temporary_password'])
status,_,_=call('/biz/catalog/games',token=fresh);check('new account forced to change initial password',status==428)
status,r,_=call('/biz/auth/password','POST',{'old_password':new['data']['temporary_password'],'new_password':'UpdatedAccount!2026'},fresh);check('new account can change generated initial password',status==200,r)
status,new_student,_=call('/biz/users','POST',{'school_id':11000,'role_key':'edu_student','nick_name':'新学生','class_id':1,'student_no':'0002','status':'0'},admin)
check('new student stable username generated',status==200 and len(new_student.get('data',{}).get('user_name',''))>20,new_student)
new_student_token=login('0002',new_student['data']['temporary_password'],'STUDENT','11000')
status,r,_=call('/biz/auth/me',token=new_student_token);check('new student receives initial-password restriction',status==200 and r['data']['must_change_pwd'])
status,before,_=call('/biz/users?size=100',token=admin)
status,r,_=call('/biz/users','POST',{'school_id':11000,'role_key':'edu_student','nick_name':'重复学号','class_id':1,'student_no':'0001','status':'0'},admin);check('duplicate student number rejected',status==409,r)
status,after,_=call('/biz/users?size=100',token=admin);check('failed user creation rolls back all account records',before['total']==after['total'])
status,r,_=call('/biz/users/20001/reset-password','POST',{},admin);check('teacher password reset succeeds',status==200,r)
status,_,_=call('/biz/auth/me',token=teacher);check('old teacher bearer rejected',status==401)
status,_,_=call('/play/planet/index.html',cookie=cookie);check('old teacher game session rejected',status==403)
status,s,_=call('/biz/catalog/schools/11000?edit=true',token=admin);s=s['data'];s['expire_time']=(datetime.now(timezone.utc)-timedelta(seconds=2)).isoformat().replace('+00:00','Z')
status,r,_=call('/biz/catalog/schools/11000','PUT',s,admin);check('school expiry fixture updated',status==200,r)
status,_,_=call('/biz/auth/me',token=student);check('already logged-in student denied after school expiry',status==403)
print('SUMMARY',len(results),'checks passed')
