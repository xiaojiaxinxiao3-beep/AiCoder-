import sys, math; sys.path.insert(0,'.')
from sim import World, accel_x, hits_player, HALF
def last(w,l,near):
    xs=[i for (i,ll) in w.b if ll==l and abs(i-near)<16]
    return max(xs) if xs else None
def predict(x,vx,vy,y,target,accel=0.0196,maxt=16):
    for k in range(1,maxt):
        vx2=vx+accel; x+=vx2; y+=vy
        if vy<0 and y<=target: return k,x
        vy=(vy-0.08)*0.98; vx=vx2*0.91
    return None,x
def simulate(cycles=7, verbose=False, behind=2, flat_slots=(9,10,11,12), ahead=2):
    w=World()
    for i in range(-12,0): w.b.add((i,-1))
    x,y,vx,vy,ground=-0.6,0.0,0.25,0.0,True
    stage='run'; t=0; T=0; placed=[]; cyc=0; m=-1; c0=0; nb=0
    while cyc<=cycles and T<900:
        act=None; jump=False; stand=round(y)-1
        if stage=='run':
            off,fwd,right,spr=45,1,1,True
            e=last(w,stand,x)
            if ground and x>=e+1-0.1: jump=True
        elif stage=='flat':
            if t<3: off,fwd,right,spr=45,1,1,True
            elif t<6: off,fwd,right,spr=135,-1,1,False
            else: off,fwd,right,spr=180,-1,0,False
            if ground and t>2: stage='up'; jump=True; m=stand; c0=math.floor(x); nb=0
            else:
                e=last(w,m,x)
                if t<=2:
                    # just off the ground: blocks on top of the layer below, ahead, where it reaches further than this level
                    c=e+1
                    if (c,m-1) in w.b and not hits_player(c,m,x,y) and c<=math.floor(x)+ahead: act=(c,m)
                else:
                    k,xl=predict(x,vx,vy,y,m+1)
                    if k is not None and e is not None:
                        n=math.floor(xl)-e
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
                    target=math.floor(xl)
                    # behind first: blocks on top of the cells before the one the jump left from
                    for c in range(c0-1,c0-1-behind,-1):
                        if act is None and nb<behind and (c,m) in w.b and (c,up) not in w.b and not hits_player(c,up,x,y): act=(c,up); nb+=1
                    if act is None and e<target+1 and x>e+1+0.01 and not hits_player(e+1,m,x,y): act=(e+1,m)
                    if act is None:
                        for c in range(c0+1,target+1):
                            if (c,m) in w.b and (c,up) not in w.b and not hits_player(c,up,x,y):
                                act=(c,up); break
        if jump:
            off,fwd,right,spr=(45,1,1,True) if stage in('flat','run') else (180,-1,0,False)
            if stage=='run': stage='flat'; m=stand; cyc+=1
            vy=0.42; t=0
            if spr: vx+=0.2*math.cos(math.radians(45))
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
        if y<m-5: print('  fell at T',T,stage,'t',t,'x=%.2f'%x); break
        t+=1; T+=1
    return w,placed,x,y,T
w,placed,x,y,T=simulate()
print('end x=%.2f y=%.2f ticks %d  blocks %d'%(x,y,T,len(placed)))
xs=[i for i,l in w.b]; top=max(l for i,l in w.b)
for l in range(top,-2,-1):
    print('%2d '%l+''.join('#' if (i,l) in w.b else '.' for i in range(-3,max(xs)+1)))
for c in range(1,6): print(' round',c,' '.join('%s%d:(%d,%d)'%(s[0],t,a[0],a[1]) for cc,s,t,a in placed if cc==c))
