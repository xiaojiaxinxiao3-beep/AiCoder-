"""Side-on simulation of the recording: x along the bridge, y up. Minecraft's own movement numbers.
A block is (i, level): it fills x in [i, i+1], y in [level, level+1]. Feet stand on level+1."""
import math, itertools, sys
HALF=0.3; EYE=1.62; REACH=4.5
def keys_at(t, sched):
    # sched: list of (from_tick, yaw_off_degrees, forward(-1,0,1), strafe_right(0/1), sprint)
    cur=sched[0]
    for s in sched:
        if t>=s[0]: cur=s
    return cur
def accel_x(off, fwd, right, ground, sprint):
    # camera forward at angle off (to the left of travel); right vector = forward turned clockwise
    th=math.radians(off)
    fx,rx=math.cos(th),math.sin(th)        # x parts of forward and right vectors
    f,s=fwd*0.98,right*0.98
    n=math.hypot(f,s)
    if n<1e-6: return 0.0
    if n>1: f,s=f/n,s/n
    base=(0.1 if ground else 0.02)*(1.3 if sprint and fwd>0 else 1.0)
    return base*(f*fx+s*rx)
class World:
    def __init__(s): s.b=set()
    def solid(s,i,l): return (i,l) in s.b
    def top_under(s,x,y0,y1):
        """Highest block top in [y1, y0] under the box centred at x (falling from feet y0 to y1)."""
        best=None
        for i in range(math.floor(x-HALF+1e-9), math.floor(x+HALF-1e-9)+1):
            for l in range(math.floor(y1)-1, math.floor(y0)+1):
                if (i,l) in s.b:
                    top=l+1
                    if y1-1e-9<=top<=y0+1e-9 and (best is None or top>best): best=top
        return best
    def wall(s,x0,x1,y):
        """Stops a move forward into a block at feet or head height."""
        if x1<=x0: return x1
        i=math.floor(x1+HALF)
        for l in (math.floor(y+1e-9), math.floor(y+1.79)):
            if (i,l) in s.b and x0+HALF<=i+1e-9: return i-HALF
        return x1
def hits_player(i,l,x,y):
    return (i < x+HALF-1e-9 and i+1 > x-HALF+1e-9 and l < y+1.8-1e-9 and l+1 > y+1e-9)
def run(sched, period, clicks, policy, cycles=6, start_gap=0.0, verbose=False, sprint=True):
    w=World()
    for i in range(-30,0): w.b.add((i,-1))            # level -1 blocks: top at y=0, ending at x=0
    for i in range(-30,-1): w.b.add((i,0))            # an upper layer one short of the end
    x,y,vx,vy,ground=-start_gap,0.0,0.2,0.0,True
    # the cycle starts at the jump tick
    log=[]; placed_all=[]
    for c in range(cycles):
        base_level=round(y)-1
        placed=[]
        for t in range(period):
            _,off,fwd,right,spr=keys_at(t,sched)
            spr=spr and sprint
            # clicks come before movement
            if t in clicks:
                act=policy(w,x,y,ground,base_level,placed,t)
                if act is not None: w.b.add(act); placed.append((t,act))
            jump=(t==0)
            if jump:
                if not ground: return ('nojump',c,t,x,y,log,placed_all)
                vy=0.42
                if spr: vx+=0.2*math.cos(math.radians(off))
                ground_now=True
            f=0.546 if ground else 0.91
            vx+=accel_x(off,fwd,right,ground,spr)
            nx=w.wall(x,x+vx,y); 
            if nx!=x+vx: vx=0
            ny=y+vy; landed=False
            if vy<=0:
                top=w.top_under(nx,y,ny)
                if top is not None: ny=top; landed=True
            x,y=nx,ny
            if landed: vy=0
            ground=landed
            vy=(vy-0.08)*0.98
            vx*=f
            log.append((c,t,round(x,3),round(y,3),ground))
            if y< base_level-3: return ('fell',c,t,x,y,log,placed_all)
        placed_all.append(placed)
    return ('ok',cycles,0,x,y,log,placed_all)
