import cv2, numpy as np, sys, pickle
exec(open('track.py').read().split("rel=[]")[0].replace("F=float(sys.argv[1]) if len(sys.argv)>1 else 160.0","F=160.0"))
# matched points for every neighbouring pair, kept so that the focal length can be varied
pairs=[]
for i in range(N-1):
    if des[i] is None or des[i+1] is None or len(kp[i])<6 or len(kp[i+1])<6: pairs.append(None); continue
    mm=bf.knnMatch(des[i],des[i+1],k=2)
    good=[a for a,b in (x for x in mm if len(x)==2) if a.distance<0.8*b.distance]
    if len(good)<6: pairs.append(None); continue
    pairs.append((np.array([kp[i][g.queryIdx].pt for g in good]),np.array([kp[i+1][g.trainIdx].pt for g in good])))
pickle.dump(pairs,open('pairs.pkl','wb'))
print('pairs',sum(p is not None for p in pairs),'of',N-1)
