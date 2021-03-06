# Gollum: A Gold Standard for Large Scale Multi Source Knowledge Graph Matching


The set of Knowledge Graphs (KGs) generated with automatic and manual approaches is constantly growing.  
For an integrated view and usage, an alignment between these KGs is necessary on the schema as well as instance level.  
There are already approaches which try to tackle this multi source knowledge graph matching problem,  
but large gold standards are missing to evaluate their effectiveness and scalability.  
In particular, most existing gold standards are fairly small and can be solved by matchers which match exactly two KGs (1:1), which are the majority of existing matching systems.  
  
We close this gap by presenting Gollum -- a gold standard for large-scale multi source knowledge graph matching with over 275,000 correspondences between 4,149 different KGs.  
They originate from knowledge graphs derived by applying the DBpedia extraction framework to a large wiki farm.  
  
Three variations of the gold standard are made available:  
(1) a version with all correspondences for evaluating unsupervised matching approaches, and two versions for evaluating supervised matching: (2) one where each KG is contained both in the train and test set, and (3) one where each KG is exclusively contained in the train or the test set.

  
We plan to extend our KG track at the Ontology Alignment Evaluation Initiative (OAEI) to allow for matching systems  
which are specifically designed to solve the multi KG matching problem.  
As a first step towards this direction, we evaluate multi source matching approaches which reuse two-KG (1:1) matchers from the past OAEI.
