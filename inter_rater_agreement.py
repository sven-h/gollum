import pandas as pd
from  nltk.metrics import agreement


df = pd.read_csv("final/survey_all.csv", header=None)


data = []
for idx, row in df.iterrows():
    #print(str(row[3]) + " - " + str(row[4]))
    data.append(("a1", idx, row[3]))
    data.append(("a2", idx, row[4]))
    data.append(("a3", idx, row[5]))
    
atask = agreement.AnnotationTask(data=data)

print("Cohen's Kappa:", atask.kappa())
print("Fleiss's Kappa:", atask.multi_kappa())
print("Krippendorf's Alpha:", atask.alpha())