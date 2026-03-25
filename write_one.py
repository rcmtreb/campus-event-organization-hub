import sys,os
BASE=r"E:\nanoR natiamtaG\thirdYear\campus-event-organization-hub"
content=sys.stdin.buffer.read()
rel=sys.argv[1]
mode="ab" if len(sys.argv)>2 and sys.argv[2]=="append" else "wb"
path=os.path.join(BASE,*rel.replace("/",os.sep).split(os.sep))
open(path,mode).write(content)
print("Wrote",len(content),"bytes (",mode,") to",path)
