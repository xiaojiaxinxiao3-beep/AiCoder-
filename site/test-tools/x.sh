export DISPLAY=:77
SP=$SP
click() { xdotool mousemove $(($1-3)) $(($2-2)); sleep 0.15; xdotool mousemove $1 $2; sleep 0.3; xdotool mousedown ${3:-1}; sleep 0.2; xdotool mouseup ${3:-1}; sleep ${4:-0.8}; }
snap() { import -window root $SP/shots/$1.png; }
chat() { xdotool key t; sleep 1.5; xdotool type --delay 60 "$1"; sleep 0.5; xdotool key Return; sleep 1.5; }
