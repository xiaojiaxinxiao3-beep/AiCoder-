import cv2, numpy as np, sys, json
V='/root/.claude/uploads/8fd84529-c46a-5acd-8a37-bac870386892/4cd4dba1-videoplayback_3.mp4'
cap=cv2.VideoCapture(V); frames=[]
while True:
    ok,f=cap.read()
    if not ok: break
    frames.append(f[100:540])          # the game picture: 360 x 440
N=len(frames); H,W=frames[0].shape[:2]
F=float(sys.argv[1]) if len(sys.argv)>1 else 160.0
CX,CY=W/2,H/2
def mask_of(f):
    b,g,r=[f[...,i].astype(np.int32) for i in range(3)]
    sand=(r>150)&(g>130)&(b<190)&(r-b>35)
    m=np.ones((H,W),np.uint8)*255
    m[sand]=0
    m=cv2.erode(m,np.ones((7,7),np.uint8))
    m[360:,:]=0            # hearts and hotbar
    m[:22,130:230]=0       # clock
    m[340:,:70]=0          # key display
    return m
def rays(p):
    v=np.column_stack([(p[:,0]-CX)/F,(p[:,1]-CY)/F,np.ones(len(p))])
    return v/np.linalg.norm(v,axis=1,keepdims=True)
def kabsch(a,b):
    Hm=a.T@b; U,S,Vt=np.linalg.svd(Hm); d=np.sign(np.linalg.det(Vt.T@U.T))
    return Vt.T@np.diag([1,1,d])@U.T      # R with b ~ R a
def fit(a,b,it=200,thr=0.012):
    best=None;rng=np.random.default_rng(0)
    if len(a)<6: return None,0
    for _ in range(it):
        idx=rng.choice(len(a),3,replace=False)
        R=kabsch(a[idx],b[idx]); err=np.linalg.norm(b-(a@R.T),axis=1); inl=err<thr
        if best is None or inl.sum()>best[1].sum(): best=(R,inl)
    R,inl=best
    if inl.sum()>=5: R=kabsch(a[inl],b[inl]); err=np.linalg.norm(b-(a@R.T),axis=1); inl=err<thr
    return R,int(inl.sum())
gray=[cv2.cvtColor(f,cv2.COLOR_BGR2GRAY) for f in frames]
masks=[mask_of(f) for f in frames]
sift=cv2.SIFT_create(nfeatures=1500,contrastThreshold=0.01)
kp=[];des=[]
for g,m in zip(gray,masks):
    k,d=sift.detectAndCompute(g,m); kp.append(k); des.append(d)
bf=cv2.BFMatcher()
def pair(i,j):
    if des[i] is None or des[j] is None or len(kp[i])<6 or len(kp[j])<6: return None,0,0
    mm=bf.knnMatch(des[i],des[j],k=2)
    good=[a for a,b in (x for x in mm if len(x)==2) if a.distance<0.8*b.distance]
    if len(good)<6: return None,0,len(good)
    pa=np.array([kp[i][g.queryIdx].pt for g in good]); pb=np.array([kp[j][g.trainIdx].pt for g in good])
    R,n=fit(rays(pa),rays(pb))
    return R,n,len(good)
rel=[]
for i in range(N-1):
    R,n,g=pair(i,i+1)
    rel.append((R,n,g))
np.save('rel_n.npy',np.array([[r[1],r[2]] for r in rel]))
import pickle; pickle.dump(rel,open('rel.pkl','wb'))
def ang(R):
    if R is None: return (None,None,None)
    # rotation of camera: small-angle read-out as yaw (about camera y), pitch (about x), roll (about z)
    rv,_=cv2.Rodrigues(R); return tuple(np.degrees(rv.ravel()))
for i,(R,n,g) in enumerate(rel):
    a=ang(R)
    print(i,'->',i+1,'inliers %3d/%3d'%(n,g), 'rot x %6.1f y %6.1f z %6.1f'%a if R is not None else 'FAILED')
