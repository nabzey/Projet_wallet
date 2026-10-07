#!/usr/bin/env python3
"""Real HTTP integration checks. Requires both services and their databases/Kafka."""
import json, os, re, time, urllib.request, urllib.error
from pathlib import Path
W=os.environ.get('WALLET_URL','http://localhost:8091')
S=os.environ.get('SERVICE_URL','http://localhost:8092')
phone='+22177'+str(time.time_ns())[-7:]
token=None
count=0
def call(base,path,body=None,expected=200,authenticated=True):
 global count
 headers={'Content-Type':'application/json'}
 if token and authenticated: headers['Authorization']='Bearer '+token
 req=urllib.request.Request(base+path,data=json.dumps(body).encode() if body is not None else None,headers=headers)
 try:
  with urllib.request.urlopen(req,timeout=15) as response: code,data=response.status,response.read()
 except urllib.error.HTTPError as e: code,data=e.code,e.read()
 assert code==expected, (path,code,data.decode())
 count+=1
 return json.loads(data) if data else None
call(W,'/api/comptes/moi',expected=401)
call(W,'/api/auth/request-otp',{'telephone':phone})
log=Path(os.environ.get('WALLET_LOG','/tmp/wallet-service.log')).read_text()
code=re.findall(r'Code envoyé à '+re.escape(phone)+r' : (\d+)',log)[-1]
call(W,'/api/auth/verify-otp',{'telephone':phone,'code':'0000'},expected=400)
verification=call(W,'/api/auth/verify-otp',{'telephone':phone,'code':code})['verificationToken']
call(W,'/api/auth/create-pin',{'telephone':phone,'pin':'1234'},expected=400)
auth=call(W,'/api/auth/create-pin',{'telephone':phone,'pin':'1234','verificationToken':verification})
token=auth['token']
call(W,'/api/auth/create-pin',{'telephone':phone,'pin':'9999','verificationToken':verification},expected=400)
call(W,'/api/auth/login',{'telephone':phone,'pin':'9999'},expected=401)
token=call(W,'/api/auth/login',{'telephone':phone,'pin':'1234'})['token']
assert call(W,'/api/comptes/moi')['solde']==0
assert call(W,'/api/transactions/depot',{'montant':50000})['nouveauSolde']==50000
call(W,'/api/transactions/depot',{'montant':-1},expected=400)
call(W,'/api/transactions/retrait',{'montant':1000,'pin':'9999'},expected=401)
call(W,'/api/transactions/retrait',{'montant':100000,'pin':'1234'},expected=400)
assert call(W,'/api/transactions/retrait',{'montant':5000,'pin':'1234'})['nouveauSolde']==45000
p=call(S,'/api/prestations',{'titre':'Site vitrine — test réel','description':'Validation Spring Boot + Kafka + Flutter','montant':20000})
call(S,f"/api/prestations/{p['id']}/payer",{'pin':'1234'})
for _ in range(20):
 p=call(S,f"/api/prestations/{p['id']}")
 if p['statut']=='PAYEE': break
 time.sleep(1)
assert p['statut']=='PAYEE',p
assert call(W,'/api/comptes/moi')['solde']==25000
call(S,f"/api/prestations/{p['id']}/payer",{'pin':'1234'},expected=400)
assert call(W,'/api/comptes/moi')['solde']==25000
assert len(call(W,'/api/comptes/moi/transactions'))==3
r=call(S,f"/api/prestations/{p['id']}/ressources",{'nom':'Awa Diop','specialite':'Développement mobile'})
assert len(call(S,f"/api/prestations/{p['id']}/ressources"))==1
from datetime import date,timedelta
t=call(S,f"/api/ressources/{r['id']}/taches",{'libelle':'Préparer la présentation','delaiRealisation':str(date.today()+timedelta(days=7))})
assert len(call(S,f"/api/ressources/{r['id']}/taches"))==1
# Failed OTP attempts remain persisted even when the request fails.
locked='+22178'+str(time.time_ns())[-7:]
call(W,'/api/auth/request-otp',{'telephone':locked})
for _ in range(3): call(W,'/api/auth/verify-otp',{'telephone':locked,'code':'0000'},expected=400)
log=Path(os.environ.get('WALLET_LOG','/tmp/wallet-service.log')).read_text()
locked_code=re.findall(r'Code envoyé à '+re.escape(locked)+r' : (\d+)',log)[-1]
call(W,'/api/auth/verify-otp',{'telephone':locked,'code':locked_code},expected=429)
print(f'OK — {count} requêtes vérifiées, paiement Kafka confirmé, solde final 25 000 FCFA.')
Path('/tmp/wallet-demo-account.json').write_text(json.dumps({'telephone':phone,'pin':'1234','numeroCompte':auth['numeroCompte']},ensure_ascii=False))
print(f'Compte de démonstration : {phone} / PIN 1234')
