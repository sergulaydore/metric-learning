hold off;
zrp = zeros(1,length(x0p));
zrn = zeros(1,length(x0n));
plot(x0p,zrp,'xb');
hold on;
plot(x0n,zrn,'xr');
xc=[0.8977610789630676 0.8977610789630676];
yc=[-1 1];
plot(xc,yc,'g');
axis([min([x0p x0n]) max([x0p x0n]) -0.1 0.1]);
print -dpng screen1337856331.png
