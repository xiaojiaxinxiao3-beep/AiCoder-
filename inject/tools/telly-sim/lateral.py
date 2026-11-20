"""The recorded table in the horizontal plane: does a sprint jump keep to the line of the bridge?
x is along the bridge, z to the right of it. A round is a level jump (lands at the same height) and a jump up (lands one higher)."""
import math, sys
YAW=[-55.1,-29.1,-23.1,-52.9,-107.7,-167.9,-179.3,-179.3,-178.7,-176.7,-173.4,-168.0,-162.2,-157.0,-150.7,-143.4,-137.4,-129.5,-111.8,-83.9,-58.9]
KEYS=["D","WD","WD","WSD","SD","SD","SD","S","S","S","S","S","S","S","S","S","SD","SD","D","WD","WD"]
def run(keys, yaw, k_flat=0, k_up=11, rounds=10, trim=0.0, verbose=False):
    x=z=0.0; y=0.0; vx=0.25; vz=0.0; vy=0.0; ground=True; k=k_flat; base=0.0; stage=1; out=[]; T=0; rnd=0; x0=0
    while rnd<rounds and T<600:
        jump=False
        if ground:
            second = stage==1 and T>0
            stage = 2 if second else 1
            k = k_up if second else k_flat
            jump=True
            if stage==1: out.append((rnd,T,x,z)); rnd+=1
        ks=keys[k]; psi=math.radians(yaw[k]+max(-10,min(10,-(z+vz*3)*40))*trim)
        f=(math.cos(psi),math.sin(psi)); r=(-math.sin(psi),math.cos(psi))
        fw=(0.98 if 'W' in ks else 0)-(0.98 if 'S' in ks else 0); st=(0.98 if 'D' in ks else 0)-(0.98 if 'A' in ks else 0)
        n=math.hypot(fw,st)
        if n>1: fw,st=fw/n,st/n
        spr='W' in ks
        a=(0.1 if ground else 0.02)*(1.3 if spr else 1.0)
        if jump:
            vy=0.42
            if spr: vx+=0.2*f[0]; vz+=0.2*f[1]
        fr=0.546 if ground else 0.91
        vx+=a*(fw*f[0]+st*r[0]); vz+=a*(fw*f[1]+st*r[1])
        x+=vx; z+=vz; ny=y+vy
        target=base+(1.0 if stage==2 else 0.0)
        landed=False
        if vy<=0 and ny<=target<=y+1e-9: ny=target; landed=True
        y=ny
        if landed: vy=0; base=target
        ground=landed; vy=(vy-0.08)*0.98; vx*=fr; vz*=fr
        if verbose: print('T%d st%d k%d yaw%.0f %s x=%.2f z=%.2f y=%.2f'%(T,stage,k,yaw[k],ks,x,z,y))
        k=(k+1)%21; T+=1
    return out,T
def report(name,keys,yaw,**kw):
    out,T=run(keys,yaw,**kw)
    if len(out)<4: print(name,'too few rounds'); return
    adv=[out[i+1][2]-out[i][2] for i in range(2,len(out)-1)]; lat=[out[i+1][3]-out[i][3] for i in range(2,len(out)-1)]; per=[out[i+1][1]-out[i][1] for i in range(2,len(out)-1)]
    print('%-46s round: %.1f ticks, %.2f blocks on, %+.2f sideways (+ is right); after %d rounds z=%+.2f'%(name,sum(per)/len(per),sum(adv)/len(adv),sum(lat)/len(lat),len(out)-1,out[-1][3]))
if __name__=='__main__':
    WK=["WD","WD"]+KEYS[2:]
    report('as measured (no W on the jump tick)',KEYS,YAW)
    report('sprint jump, table as it is (yaw -55 at the jump)',WK,YAW)
    for sh in (1,2,3):
        # the jump falls sh ticks later in the table: the camera is already further forward
        ky=[YAW[(i+sh)%21] for i in range(21)]; kk=[WK[(i+sh)%21] for i in range(21)]
        kk=["WD" if i<2 else kk[i] for i in range(21)]
        report('sprint jump %d tick(s) later in the swing (yaw %.0f)'%(sh,ky[0]),kk,ky,k_up=(11-sh))
    for y0 in (-45,-35,-25,-15,0):
        yy=[y0 if i==0 else YAW[i] for i in range(21)]
        report('sprint jump with the camera at %d on that tick'%y0,WK,yy)
