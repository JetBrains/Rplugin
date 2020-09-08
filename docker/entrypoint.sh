#!/bin/sh

Xvfb :1 -screen 0 1280x1024x24 >> Xvfb.out 2>&1 &
$@
