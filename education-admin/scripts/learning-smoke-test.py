#!/usr/bin/env python3
"""Mutating HTTP acceptance checks for a fresh disposable dev process only."""
import argparse
import io
import json
import os
import urllib.error
import urllib.parse
import urllib.request
import uuid
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('--base', default='http://127.0.0.1:8081')
parser.add_argument('--allow-development', action='store_true')
args = parser.parse_args()
url = urllib.parse.urlparse(args.base)
if not args.allow_development or url.hostname not in ('127.0.0.1', 'localhost', '::1'):
    parser.error('Only an explicitly authorized disposable loopback development backend is supported')
password = os.environ['EDU_TEST_ADMIN_PASSWORD']
user_password = os.environ['EDU_TEST_DEMO_PASSWORD']

def request(method, path, data=None, token=None, expected=200, binary=False):
    headers = {}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    if data is not None:
        headers['Content-Type'] = 'application/json'
    req = urllib.request.Request(args.base.rstrip('/') + path, method=method,
                                 data=json.dumps(data).encode() if data is not None else None, headers=headers)
    try:
        response = urllib.request.urlopen(req, timeout=20)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        payload = response.read()
        assert response.status == expected, (method, path, response.status, payload.decode(errors='replace')[:500])
        if binary:
            assert zipfile.is_zipfile(io.BytesIO(payload)), path
            return payload
        result = json.loads(payload)
        if expected == 200:
            assert result.get('code', 200) == 200, (path, result)
        return result

def login(name, pwd):
    return request('POST', '/biz/auth/login', {'mode': 'ACCOUNT', 'username': name, 'password': pwd})['token']

root = login('admin', password)
request('POST', '/biz/auth/password', {'old_password': password, 'new_password': 'SmokeAdmin!2026'}, root)
root = login('admin', 'SmokeAdmin!2026')
suffix = uuid.uuid4().hex[:8]
opts = request('GET', '/biz/options', token=root)['data']
school = opts['schools'][0]['school_id']
cid = opts['classes'][0]['class_id']
course = request('GET', '/biz/catalog/courseware', token=root)['rows'][0]['courseware_id']
assert request('GET', '/biz/student-registration/tree')['data']
request('POST', '/biz/student-registration', {'school_id': school, 'class_id': cid, 'student_no': suffix,
        'nick_name': '验收学生', 'password': 'StudentSmoke!2026', 'role_key': 'admin', 'role_ids': [1]})
registered = request('GET', '/biz/users?keyword=' + urllib.parse.quote('验收学生'), token=root)['rows'][0]
assert registered['role_keys'] == ['edu_student']
assert request('GET', '/biz/learning/groups?class_id=' + str(cid), token=root)['data']
material = request('POST', '/biz/learning/dubbing/materials', {'title': '验收素材' + suffix, 'volume': 1,
        'video_url': 'https://example.com/material.mp4', 'material_text': 'A test sentence.', 'material_tag': '01,03', 'status': '0'}, root)['data']['id']
reading = request('POST', '/biz/catalog/courseware', {'courseware_name': '验收阅读' + suffix,
        'subject_code': 'READING', 'volume': 1, 'stage_code': 'BASIC', 'status': 'PUBLISHED',
        'extension': {'dubbing_material_id': material, 'config_json': '{"steps":[]}'}}, root)['data']['id']
preview = request('GET', '/biz/catalog/courseware/' + str(reading), token=root)['data']
assert str(preview['extension']['dubbing_material_id']) == str(material)
request('POST', '/biz/courseware/' + str(course) + '/customize', {'school_id': school}, root)
batch = request('POST', '/biz/learning/activation/batches', {'batch_name': '验收批次' + suffix,
        'permission_type': '3', 'permission_range': '{}', 'valid_days': 180, 'total_num': 2,
        'expire_time': '2099-01-01T00:00:00Z'}, root)['data']['id']
code = request('GET', '/biz/learning/activation/batches/' + str(batch) + '/codes', token=root)['data'][0]['code']
request('POST', '/biz/learning/activation/batches/' + str(batch) + '/export', token=root, binary=True)
student = login('student', user_password)
request('POST', '/biz/learning/activation/redeem', {'code': code}, student)
request('POST', '/biz/learning/activation/redeem', {'code': code}, student, expected=400)
request('POST', '/biz/learning/activation/batches', {}, student, expected=403)
request('POST', '/biz/learning/dubbing/score', {'material_id': material, 'total_score': 100}, student, expected=503)
teacher = login('teacher', user_password)
request('POST', '/biz/learning/pk/match', {'class_id': cid, 'courseware_id': course, 'mode': 'PERSONAL', 'stage': 'BASIC'}, teacher)
rounds = request('GET', '/biz/learning/pk?' + urllib.parse.urlencode({'class_id': cid, 'courseware_id': course}), token=teacher)['data']
match = next(m for m in rounds[0]['matches'] if m['right_id'])
request('POST', '/biz/learning/pk/' + str(match['match_id']) + '/result', {'winner_id': match['left_id']}, teacher)
request('POST', '/biz/learning/pk/' + str(match['match_id']) + '/result', {'winner_id': None}, teacher)
request('POST', '/biz/learning/scores/all-plus', {'class_id': cid, 'courseware_id': course}, teacher)
request('POST', '/biz/learning/records/export', token=root, binary=True)
request('POST', '/biz/users/batch/template', token=root, binary=True)
other_class = request('POST', '/biz/catalog/classes', {'school_id': school, 'class_name': '隔离班' + suffix,
        'academic_year': '2026-2027', 'grade': 1, 'status': 'ACTIVE'}, root)['data']['id']
request('GET', '/biz/learning/groups?class_id=' + str(other_class), token=teacher, expected=403)
print('PASS: student-only registration, default groups, reading extensions, customization, activation redemption, Excel exports, teacher PK, score reversal, batch scoring, and class isolation.')
