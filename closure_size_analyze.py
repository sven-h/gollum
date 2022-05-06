from numpy import genfromtxt
x=genfromtxt('closureSize.csv',delimiter=';')

import matplotlib.pyplot as plt
# use LaTeX fonts in the plot
plt.rc('text', usetex=True)
plt.rc('font', family='serif')

plt.figure(figsize=(15,5))

plt.hist(x, bins=40)  # density=False would make counts , bins=30
plt.ylabel('Count')
plt.xlabel('Size of identity set ')


# save as PDF
plt.savefig("closure_size_analyze.pdf", bbox_inches='tight')


import pandas as pd
sr = pd.Series(x)
print(sr.describe())


#count    102854.000000
#mean          2.611955
#std           1.765623
#min           2.000000
#25%           2.000000
#50%           2.000000
#75%           3.000000
#max          75.000000