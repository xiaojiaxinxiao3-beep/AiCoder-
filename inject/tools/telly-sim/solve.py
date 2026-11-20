import numpy as np, pickle, sys, math
pairs=pickle.load(open('pairs.pkl','rb')); N=len(pairs)+1
W,H=360,440; CX,CY=W/2,H/2
def rays(p,F):
    v=np.column_stack([(p[:,0]-CX)/F,(p[:,1]-CY)/F,np.ones(len(p))]); return v/np.linalg.norm(v,axis=1,keepdims=True)
def kabsch(a,b):
    Hm=a.T@b; U,S,Vt=np.linalg.svd(Hm); d=np.sign(np.linalg.det(Vt.T@U.T)); return Vt.T@np.diag([1,1,d])@U.T
def fit(a,b,it=300,thr=0.012,seed=0):
    rng=np.random.default_rng(seed); best=None
    for _ in range(it):
        idx=rng.choice(len(a),3,replace=False); R=kabsch(a[idx],b[idx]); inl=np.linalg.norm(b-(a@R.T),axis=1)<thr
        if best is None or inl.sum()>best[1].sum(): best=(R,inl)
    R,inl=best
    if inl.sum()>=4: R=kabsch(a[inl],b[inl]); inl=np.linalg.norm(b-(a@R.T),axis=1)<thr
    return R,int(inl.sum())
def Rx(t): c,s=math.cos(t),math.sin(t); return np.array([[1,0,0],[0,c,-s],[0,s,c]])
def Ry(t): c,s=math.cos(t),math.sin(t); return np.array([[c,0,s],[0,1,0],[-s,0,c]])
def chain(F):
    rel=[]
    for p in pairs:
        if p is None: rel.append((np.eye(3),0)); continue
        rel.append(fit(rays(p[0],F),rays(p[1],F)))
    return rel
def angles(rel,p0):
    # camera coordinates: x right, y down, z forward. C takes world to camera; the world's up is -y at level.
    C=Rx(math.radians(p0)); out=[]
    for i in range(N):
        up=C@np.array([0,-1,0.0])          # the world's up as the camera sees it
        fwd=C.T@np.array([0,0,1.0])        # where the camera looks, in the world
        pitch=math.degrees(math.asin(max(-1,min(1,fwd[1]))))           # + is down
        yaw=math.degrees(math.atan2(fwd[0],fwd[2]))
        roll=math.degrees(math.atan2(up[0],-up[1]))
        out.append((yaw,pitch,roll))
        if i<N-1: C=rel[i][0]@C
    return np.array(out)
best=None
for F in (130,145,160,175,190,210):
    rel=chain(F)
    for p0 in range(-40,61,2):
        a=angles(rel,p0); cost=np.mean(np.abs(a[:150,2]))
        if best is None or cost<best[0]: best=(cost,F,p0)
    print('F',F,'best p0/roll so far',best)
cost,F,p0=best
rel=chain(F); a=angles(rel,p0)
yaw=np.degrees(np.unwrap(np.radians(a[:,0])))
np.save('angles.npy',np.column_stack([yaw,a[:,1],a[:,2],[r[1] for r in rel]+[0]]))
print('F',F,'p0',p0,'mean |roll| %.1f'%cost)
for i in range(0,150): print(i,'yaw %7.1f pitch %6.1f roll %5.1f inl %d'%(yaw[i],a[i,1],a[i,2],rel[i][1] if i<N-1 else 0))
