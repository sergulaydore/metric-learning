w = [4.05541950635598,1.4605603819111388,-18.77614070325683,-0.2508174807180547,18.778347511603283,11.216426240786346,]
theta = 1.0
var('x,y,z')
p1 = implicit_plot3d((w[0] * x**2 + w[1] * y**2 + w[2] * z**2 + w[3] * sqrt(2) * x*y) + (w[4] * sqrt(2) * x*z + w[5] * sqrt(2) * y*z) == theta, (x, 0, 1), (y, 0, 1), (z, 0, 1))
p2 = point3d(negs[0:4999],size=10,color='red')
p3 = point3d(poss,size=10,color='blue')
show(p1+p2+p3)
