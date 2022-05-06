from numpy import genfromtxt
x=genfromtxt('transitive_confidences.csv',delimiter=';')

import matplotlib.pyplot as plt
# use LaTeX fonts in the plot
plt.rc('text', usetex=True)
plt.rc('font', family='serif')

plt.figure(figsize=(15,5))

plt.hist(x, bins=40)  # density=False would make counts , bins=30
plt.ylabel('\#Links')
plt.xlabel('Confidence')


# save as PDF
plt.savefig("transitive_confidences.pdf", bbox_inches='tight')