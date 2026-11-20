import sys, math; sys.path.insert(0,'.')
from sim import World, accel_x, hits_player, HALF
YAW=[-55.1,-29.1,-23.1,-52.9,-107.7,-167.9,-179.3,-179.3,-178.7,-176.7,-173.4,-168.0,-162.2,-157.0,-150.7,-143.4,-137.4,-129.5,-111.8,-83.9,-58.9]
KEYS=["D","WD","WD","WSD","SD","SD","SD","S","S","S","S","S","S","S","S","S","SD","SD","D","WD","WD"]
SECOND=11
def last(w,l,near):
    xs=[i for (i,ll) in w.b if ll==l and abs(i-near)<16]
    return max(xs) if xs else None
def predict(x,vx,vy,y,target,accel=0.0196,maxt=16):
    for k in range(1,maxt):
        vx2=vx+accel; x+=vx2; y+=vy
        if vy<0 and y<=target: return k,x
        vy=(vy-0.08)*0.98; vx=vx2*0.91
    return None,x
def simulate(keys=KEYS, rounds=8, rule="java", verbose=False, behind=1, lowlimit=1, first=0, early=0, higher=0):
    w=World()
    for i in range(-12,0): w.b.add((i,-1))
    x,y,vx,vy,ground=-0.6,0.0,0.25,0.0,True
    stage=0; k=0; T=0; placed=[]; m=-1; c0=0; nb=0; rnd=0; c1=0
    while rnd<=rounds and T<900:
        act=None; stand=round(y)-1; jump=False
        if stage==0:
            off,fwd,right,spr=0,1,0,True
            e=last(w,stand,x)
            if ground and x>=e+1-0.1: jump=True; stage=1; m=stand; k=2; rnd+=1; c1=math.floor(x)
        else:
            if ground:
                second=6<=k<=15
                jump=True; stage=2 if second else 1; m=stand; c1=math.floor(x); k=SECOND if second else first; c0=math.floor(x); nb=0
                if not second: rnd+=1
            ks=keys[k]; off=-YAW[k]
            fwd=(1 if 'W' in ks else 0)-(1 if 'S' in ks else 0); right=1 if 'D' in ks else 0; spr='W' in ks
            kk=k; k=(k+1)%21
            if not ground:
                if stage==1:
                    e=last(w,m,x)
                    if kk<=3:
                        c=e+1
                        if c<=math.floor(x)+2 and (c,m-1) in w.b and not hits_player(c,m,x,y): act=(c,m)
                    else:
                        kk2,xl=predict(x,vx,vy,y,m+1)
                        if kk2 is not None:
                            n=math.floor(xl)-e
                            if n>0 and kk2<=n+early and x>e+1+0.01 and not hits_player(e+1,m,x,y): act=(e+1,m)
                            elif n<=0 and early:
                                # the row reaches under the landing: blocks on top of the cells behind it,
                                # the further one first (the nearer would hide it)
                                lim=math.floor(xl-0.35)-1
                                for c in (lim-1,lim):
                                    if act is None and c>c1 and (c,m) in w.b and (c,m+1) not in w.b and not hits_player(c,m+1,x,y): act=(c,m+1)
                else:
                    up=m+1
                    kk2,xl=predict(x,vx,vy,y,up+1)
                    e=last(w,m,x)
                    if kk2 is None:
                        if vy<0 and x>e+1+0.01 and not hits_player(e+1,m,x,y): act=(e+1,m)
                    else:
                        target=math.floor(xl)
                        if (c0-1,up) in w.b: nb=behind
                        for c in range(c0-1,c0-1-behind,-1):
                            if act is None and nb<behind and (c,m) in w.b and (c,up) not in w.b and not hits_player(c,up,x,y): act=(c,up); nb+=1
                        if act is None and e<target+lowlimit and x>e+1+0.01 and not hits_player(e+1,m,x,y): act=(e+1,m)
                        if act is None and higher and (c0-1,up) in w.b and (c0-1,up+1) not in w.b and y>=up+1 and not hits_player(c0-1,up+1,x,y): act=(c0-1,up+1)
                        if act is None:
                            for c in range(c0+1,target+1):
                                if (c,m) in w.b and (c,up) not in w.b and not hits_player(c,up,x,y): act=(c,up); break
        if jump:
            vy=0.42
            if spr: vx+=0.2*math.cos(math.radians(off))
        if act: w.b.add(act); placed.append((rnd,stage,T,act))
        f=0.546 if ground else 0.91
        vx+=accel_x(off,fwd,right,ground,spr)
        nx=x+vx; ny=y+vy; landed=False
        if vy<=0:
            tp=w.top_under(nx,y,ny)
            if tp is not None: ny=tp; landed=True
        x,y=nx,ny
        if landed: vy=0
        ground=landed; vy=(vy-0.08)*0.98; vx*=f
        if verbose: print('  T%d st%d k%d x=%.2f y=%.2f g=%s act=%s'%(T,stage,k,x,y,ground,act))
        if y<m-5: print('  fell at T',T,'stage',stage,'k',k,'x=%.2f'%x); break
        T+=1
    return w,placed,x,y,T
def show(w):
    xs=[i for i,l in w.b]; top=max(l for i,l in w.b)
    for l in range(top,-2,-1): print('%2d '%l+''.join('#' if (i,l) in w.b else '.' for i in range(-3,max(xs)+1)))
if __name__=='__main__':
    for name,keys in (('table as measured',KEYS),('W held at the jump',["WD","WD"]+KEYS[2:])):
        w,placed,x,y,T=simulate(keys=keys)
        print(name,': end x=%.2f y=%.2f ticks %d blocks %d'%(x,y,T,len(placed)))
        show(w)
