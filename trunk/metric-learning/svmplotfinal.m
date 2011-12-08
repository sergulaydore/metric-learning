if w2 < 1E-2
w2 = w1;
w1 = 0;
q = -(-10.330285022021892)/w2;
xTp = x2p;
x2p = x1p;
x1p = xTp;
xTn = x2n;
x2n = x1n;
x1n = xTn;
else
q = -(-10.330285022021892)/w2;
end
plot3(x0p,x1p,x2p,'xb'); hold on; plot3(x0n,x1n,x2n,'xr');
x = [0:0.025:1];
[xx,yy] = meshgrid(x,x);
zz = -1.0 * w0/w2 * xx + -1.0 * w1/w2 * yy + q;
mesh(xx,yy,zz);
axis([0 1 0 1 0 1]);
view(-37.5, 10);
print -dpng screen1323351392.png
