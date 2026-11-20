import sys, math; sys.path.insert(0,'.')
from sim import World, accel_x, hits_player, HALF
def last(w,l,near):
    xs=[i for (i,ll) in w.b if ll==l and abs(i-near)<14]
    return max(xs) if xs else None
def predict(x,vx,vy,y,target,accel=0.0196,maxt=16):
    """Ticks until the feet come down to height target, and x then."""
    for k in range(1,maxt):
        vx2=vx+accel; x+=vx2; y+=vy
        if vy<0 and y<=target: return k,x
        vy=(vy-0.08)*0.98; vx=vx2*0.91
    return None,x
def simulate(cycles=8, verbose=False):
    w=World()
    for i in range(-30,0): w.b.add((i,-1))
    x,y,vx,vy,ground=-0.6,0.0,0.25,0.0,True
    stage='run'; t=0; T=0; placed=[]; cyc=0; jumps=[]; m=-1
    while cyc<=cycles and T<700:
        act=None; jump=False
        stand=round(y)-1
        if stage=='run':
            off,fwd,right,spr=45,1,1,True
            e=last(w,stand,x)
            if ground and x>=e+1-0.1: jump=True
        elif stage=='flat':
            # facing forward for three ticks, then round; back and strafe, then back alone
            if t<3: off,fwd,right,spr=45,1,1,True
            elif t<6: off,fwd,right,spr=135,-1,1,False
            else: off,fwd,right,spr=180,-1,0,False
            if ground and t>2: stage='up'; jump=True; m=stand
            else:
                e=last(w,m,x)
                k,xl=predict(x,vx,vy,y,m+1)
                if k is not None and e is not None:
                    need=math.floor(xl)            # the cell you come down in
                    n=need-e
                    if n>0 and k<=n and x>e+1+0.01 and not hits_player(e+1,m,x,y): act=(e+1,m)
        if stage=='up' and not jump:
            if t<=3: off,fwd,right,spr=180,-1,0,False
            elif t<=5: off,fwd,right,spr=135,-1,1,False
            elif t==6: off,fwd,right,spr=90,0,1,False
            else: off,fwd,right,spr=45,1,1,True
            if ground and t>2: stage='flat'; jump=True; cyc+=1; m=stand
            else:
                up=m+1
                k,xl=predict(x,vx,vy,y,up+1)
                e=last(w,m,x)
                if k is not None:
                    cstar=math.floor(xl)
                    # the lower level under where you come down, then the blocks on top, the one under you last
                    if e<cstar and x>e+1+0.01 and not hits_player(e+1,m,x,y): act=(e+1,m)
                    else:
                        for c in (cstar-2,cstar-1,cstar):
                            if (c,m) in w.b and (c,up) not in w.b and not hits_player(c,up,x,y):
                                if c==cstar and k>1: continue      # the one you land on goes in last
                                act=(c,up); break
        if jump:
            off,fwd,right,spr=(45,1,1,True) if stage in('flat','run') else (180,-1,0,False)
            if stage=='run': stage='flat'; m=stand; cyc+=1
            vy=0.42; t=0
            if spr: vx+=0.2*math.cos(math.radians(45))
            jumps.append((T,stage,round(x,2),round(y,2)))
        if act: w.b.add(act); placed.append((cyc,stage,t,act))
        f=0.546 if ground else 0.91
        vx+=accel_x(off,fwd,right,ground,spr)
        nx=x+vx; ny=y+vy; landed=False
        if vy<=0:
            tp=w.top_under(nx,y,ny)
            if tp is not None: ny=tp; landed=True
        x,y=nx,ny
        if landed: vy=0
        ground=landed; vy=(vy-0.08)*0.98; vx*=f
        if verbose and cyc<=2: print('  T%d %s t%d x=%.2f y=%.2f g=%s act=%s'%(T,stage,t,x,y,ground,act))
        if y<m-5: print('  fell at T',T,stage,'t',t,'x=%.2f'%x); break
        t+=1; T+=1
    return jumps,placed,x,y
jumps,placed,x,y=simulate(verbose='-v' in sys.argv)
print('end x=%.2f y=%.2f'%(x,y))
print('jumps:',' '.join('%d:%s@%.2f,%.0f'%j for j in jumps[:20]))
for c in range(1,8):
    print(' cycle',c,' '.join('%s%d:(%d,%d)'%(s[0],t,a[0],a[1]) for cc,s,t,a in placed if cc==c))
