# Review sample: Machine Learning Engineering

Please mark each question OK / FIX / REMOVE and write corrections in the right-hand column of the sign-off sheet.
40 questions drawn with a fixed seed from 210. The tag after each wrong option is the misconception it is meant to reveal.

**1. [ml.imbalanced.q3] (difficulty 3)** Class weights in a loss function
   A) change the labels  <- reveals: Believes undersampling never loses information
   B) reduce the number of features  <- reveals: Believes undersampling never loses information
   C) make errors on the minority class more costly  <- CORRECT
   D) remove the majority class  <- reveals: Believes undersampling never loses information

**2. [ml.gradient_descent.q5] (difficulty 3)** Why does feature scaling help gradient descent?
   A) It makes the loss surface rounder so convergence is faster  <- CORRECT
   B) It changes the labels  <- reveals: Believes a larger learning rate always converges faster
   C) It increases the learning rate  <- reveals: Believes a larger learning rate always converges faster
   D) It removes the need for a loss  <- reveals: Believes a larger learning rate always converges faster

**3. [ml.attention.q5] (difficulty 5)** A causal mask in a decoder prevents
   A) tokens from attending to past tokens  <- reveals: Believes attention weights need not sum to one
   B) the use of residuals  <- reveals: Believes attention weights need not sum to one
   C) the use of softmax  <- reveals: Believes attention weights need not sum to one
   D) tokens from attending to future tokens  <- CORRECT

**4. [ml.attention.q4] (difficulty 5)** Self-attention compute and memory cost grows with sequence length n as roughly
   A) O(n^2)  <- CORRECT
   B) O(n)  <- reveals: Believes attention cost is linear in sequence length
   C) O(log n)  <- reveals: Believes attention cost is linear in sequence length
   D) O(1)  <- reveals: Believes attention cost is linear in sequence length

**5. [ml.train_test.q5] (difficulty 1)** The validation set is used to
   A) replace the training set  <- reveals: Evaluates the model on the training data and reports that accuracy
   B) give the final unbiased score  <- reveals: Evaluates the model on the training data and reports that accuracy
   C) choose hyperparameters and compare models  <- CORRECT
   D) store the model  <- reveals: Evaluates the model on the training data and reports that accuracy

**6. [ml.data_eda.q1] (difficulty 1)** In a dataset each row usually represents
   A) one feature  <- reveals: Skips exploration and goes straight to model training
   B) one example or observation  <- CORRECT
   C) a model  <- reveals: Skips exploration and goes straight to model training
   D) the target column  <- reveals: Skips exploration and goes straight to model training

**7. [ml.serving.q4] (difficulty 4)** A fallback or timeout in serving helps
   A) hide errors from developers  <- reveals: Believes a model that works in a notebook needs no extra work to serve it
   B) increase accuracy  <- reveals: Believes a model that works in a notebook needs no extra work to serve it
   C) degrade gracefully when the model fails or is slow  <- CORRECT
   D) remove monitoring  <- reveals: Believes a model that works in a notebook needs no extra work to serve it

**8. [ml.svm.q6] (difficulty 4)** A very large gamma in an RBF kernel tends to
   A) a straight boundary  <- reveals: Believes a larger C always generalises better
   B) create a wiggly, overfit boundary  <- CORRECT
   C) a wider margin always  <- reveals: Believes a larger C always generalises better
   D) no boundary  <- reveals: Believes a larger C always generalises better

**9. [ml.cross_validation.q3] (difficulty 3)** Which is a hyperparameter?
   A) the learned bias  <- reveals: Confuses hyperparameters with learned parameters
   B) the predicted label  <- reveals: Confuses hyperparameters with learned parameters
   C) the maximum depth of a tree  <- CORRECT
   D) the weights of a linear model  <- reveals: Confuses hyperparameters with learned parameters

**10. [ml.mlops_ci.q3] (difficulty 3)** A test that checks predicted probabilities sum to one is an example of
   A) an invariant test  <- CORRECT
   B) a licence check  <- reveals: Believes ML code does not need automated tests
   C) a performance test  <- reveals: Believes ML code does not need automated tests
   D) a data leak  <- reveals: Believes ML code does not need automated tests

**11. [ml.rnn_seq.q3] (difficulty 4)** LSTM gates are designed to
   A) control what is remembered and forgotten to ease long dependencies  <- CORRECT
   B) add more weights per step only  <- reveals: Believes a plain RNN handles very long dependencies well
   C) parallelise time steps  <- reveals: Believes RNNs can be trained fully in parallel across time steps
   D) remove the hidden state  <- reveals: Believes a plain RNN handles very long dependencies well

**12. [ml.pca.q2] (difficulty 4)** PCA is
   A) supervised, using labels  <- reveals: Believes PCA uses the target labels
   B) a classification model  <- reveals: Believes PCA uses the target labels
   C) unsupervised, using only the features  <- CORRECT
   D) a clustering algorithm only  <- reveals: Believes PCA uses the target labels

**13. [ml.train_test.q6] (difficulty 1)** Stratified splitting means
   A) removing minority classes  <- reveals: Evaluates the model on the training data and reports that accuracy
   B) keeping class proportions similar across splits  <- CORRECT
   C) using the test set twice  <- reveals: Evaluates the model on the training data and reports that accuracy
   D) splitting by feature scale  <- reveals: Evaluates the model on the training data and reports that accuracy

**14. [ml.backprop.q5] (difficulty 4)** Which helps against vanishing gradients?
   A) ReLU activations and residual connections  <- CORRECT
   B) zero initialisation  <- reveals: Believes vanishing gradients occur because gradients are too large
   C) more sigmoid layers  <- reveals: Believes vanishing gradients occur because gradients are too large
   D) removing the loss  <- reveals: Believes vanishing gradients occur because gradients are too large

**15. [ml.feature_eng.q1] (difficulty 3)** Which is a leaking feature when predicting loan default at application time?
   A) the applicant's income  <- reveals: Uses a feature that is not available at prediction time
   B) the applicant's age  <- reveals: Uses a feature that is not available at prediction time
   C) the loan amount requested  <- reveals: Uses a feature that is not available at prediction time
   D) a column recording whether the loan was later written off  <- CORRECT

**16. [ml.regularization.q5] (difficulty 3)** The regularisation strength is best chosen using
   A) a fixed value of 1000  <- reveals: Believes a larger regularisation strength always improves test performance
   B) cross-validation or a validation set  <- CORRECT
   C) the test set repeatedly  <- reveals: Believes a larger regularisation strength always improves test performance
   D) the training loss only  <- reveals: Believes a larger regularisation strength always improves test performance

**17. [ml.clustering.q6] (difficulty 3)** Which algorithm can find arbitrary-shaped clusters and mark noise points?
   A) linear regression  <- reveals: Believes k-means always finds the global optimum
   B) PCA  <- reveals: Believes k-means always finds the global optimum
   C) k-means  <- reveals: Believes k-means always finds the global optimum
   D) DBSCAN  <- CORRECT

**18. [ml.rnn_seq.q4] (difficulty 4)** An RNN uses
   A) only biases  <- reveals: Believes an RNN uses different weights at each time step
   B) the same weights at every time step  <- CORRECT
   C) new weights at every step  <- reveals: Believes an RNN uses different weights at each time step
   D) no weights  <- reveals: Believes an RNN uses different weights at each time step

**19. [ml.pca.q6] (difficulty 4)** A limitation of PCA is that
   A) it only works on text  <- reveals: Believes PCA uses the target labels
   B) it cannot reduce dimensions  <- reveals: Believes PCA uses the target labels
   C) it needs labels  <- reveals: Believes PCA uses the target labels
   D) the top-variance directions may not be the most predictive  <- CORRECT

**20. [ml.pipelines.q1] (difficulty 3)** To make an experiment reproducible you should record
   A) the date only  <- reveals: Believes a single best run is a sufficient result to report
   B) only the final accuracy  <- reveals: Believes setting a seed once guarantees identical results across different library versions and hardware
   C) seeds, library versions, data version and code version  <- CORRECT
   D) nothing, models are deterministic  <- reveals: Believes setting a seed once guarantees identical results across different library versions and hardware

**21. [ml.metrics_cls.q6] (difficulty 2)** For screening a serious disease where missing a case is costly you want
   A) high precision only  <- reveals: Swaps the definitions of precision and recall
   B) low recall  <- reveals: Swaps the definitions of precision and recall
   C) high accuracy only  <- reveals: Believes high accuracy always means a good model
   D) high recall  <- CORRECT

**22. [ml.mlops_ci.q2] (difficulty 3)** Continuous integration for an ML repo should include
   A) only manual checks before the deadline  <- reveals: Believes ML code does not need automated tests
   B) only model training  <- reveals: Believes ML code does not need automated tests
   C) nothing until release  <- reveals: Believes ML code does not need automated tests
   D) unit tests and data validation on every push  <- CORRECT

**23. [ml.data_eda.q6] (difficulty 1)** For skewed data with outliers, a better summary of the typical value is the
   A) maximum  <- reveals: Believes the mean is robust to outliers
   B) median  <- CORRECT
   C) sum  <- reveals: Believes the mean is robust to outliers
   D) variance  <- reveals: Believes the mean is robust to outliers

**24. [ml.rnn_seq.q5] (difficulty 4)** Why is RNN training slow compared with transformers?
   A) it has more attention  <- reveals: Believes RNNs can be trained fully in parallel across time steps
   B) it needs no GPU  <- reveals: Believes RNNs can be trained fully in parallel across time steps
   C) time steps must be processed sequentially  <- CORRECT
   D) it has no gradients  <- reveals: Believes RNNs can be trained fully in parallel across time steps

**25. [ml.cnn.q2] (difficulty 4)** Compared with a fully connected layer on an image, a convolution layer has
   A) more parameters  <- reveals: Believes each position in a convolution layer has its own separate weights
   B) no parameters  <- reveals: Believes each position in a convolution layer has its own separate weights
   C) identical parameters  <- reveals: Believes each position in a convolution layer has its own separate weights
   D) far fewer parameters  <- CORRECT

**26. [ml.svm.q4] (difficulty 4)** The kernel trick allows an SVM to
   A) run without data  <- reveals: Believes SVMs do not need feature scaling
   B) learn nonlinear boundaries without explicitly mapping features  <- CORRECT
   C) skip the margin  <- reveals: Believes SVMs do not need feature scaling
   D) avoid scaling  <- reveals: Believes SVMs do not need feature scaling

**27. [ml.transfer.q5] (difficulty 4)** A frozen pretrained model used as a feature extractor
   A) does not update the pretrained weights  <- CORRECT
   B) updates all weights  <- reveals: Believes fine-tuning always requires updating all weights
   C) is trained from random initial weights  <- reveals: Believes fine-tuning always requires updating all weights
   D) cannot be used on new data  <- reveals: Believes fine-tuning always requires updating all weights

**28. [ml.pca.q4] (difficulty 4)** Explained variance ratio tells you
   A) the share of total variance each component keeps  <- CORRECT
   B) the learning rate  <- reveals: Believes PCA uses the target labels
   C) the accuracy of the model  <- reveals: Believes PCA uses the target labels
   D) the number of classes  <- reveals: Believes PCA uses the target labels

**29. [ml.metrics_cls.q2] (difficulty 2)** Precision equals
   A) (TP + TN) / total  <- reveals: Believes high accuracy always means a good model
   B) TP / (TP + FP)  <- CORRECT
   C) TP / (TP + FN)  <- reveals: Swaps the definitions of precision and recall
   D) TN / (TN + FP)  <- reveals: Swaps the definitions of precision and recall

**30. [ml.feature_eng.q3] (difficulty 3)** A log transform is often useful for
   A) categorical text  <- reveals: Computes target encoding on all data without folds
   B) labels  <- reveals: Computes target encoding on all data without folds
   C) binary flags  <- reveals: Computes target encoding on all data without folds
   D) right-skewed positive features  <- CORRECT

**31. [ml.knn.q5] (difficulty 2)** Prediction cost of plain k-NN on n training points is about
   A) O(log log n)  <- reveals: Believes k-NN prediction is faster than training a neural network on large data
   B) O(1)  <- reveals: Believes k-NN prediction is faster than training a neural network on large data
   C) zero  <- reveals: Believes k-NN prediction is faster than training a neural network on large data
   D) O(n) distance computations per query  <- CORRECT

**32. [ml.linear_reg.q3] (difficulty 2)** To fit a curved relationship with a linear model you can
   A) remove the bias  <- reveals: Believes linear regression can only fit straight lines even with polynomial features
   B) it is impossible  <- reveals: Believes linear regression can only fit straight lines even with polynomial features
   C) add polynomial features such as x squared  <- CORRECT
   D) switch to accuracy  <- reveals: Believes linear regression can only fit straight lines even with polynomial features

**33. [ml.nn_basics.q2] (difficulty 3)** ReLU outputs
   A) x squared  <- reveals: Uses softmax for a regression output
   B) max(0, x)  <- CORRECT
   C) always 1  <- reveals: Uses softmax for a regression output
   D) 1 / (1 + e^-x)  <- reveals: Uses softmax for a regression output

**34. [ml.data_eda.q2] (difficulty 1)** A single huge outlier will affect which statistic most?
   A) the mean  <- CORRECT
   B) the median  <- reveals: Believes the mean is robust to outliers
   C) the mode  <- reveals: Believes the mean is robust to outliers
   D) the count of rows  <- reveals: Believes the mean is robust to outliers

**35. [ml.linear_algebra.q2] (difficulty 2)** Is A times B always equal to B times A for matrices?
   A) No, matrix multiplication is generally not commutative  <- CORRECT
   B) Yes if the entries are positive  <- reveals: Believes matrix multiplication is commutative
   C) Yes if both are square  <- reveals: Believes matrix multiplication is commutative
   D) Yes, always  <- reveals: Believes matrix multiplication is commutative

**36. [ml.trees.q4] (difficulty 3)** Which controls tree overfitting?
   A) increase the depth indefinitely  <- reveals: Believes an unrestricted deep tree generalises best
   B) scale the features  <- reveals: Believes decision trees need feature scaling
   C) use more splits  <- reveals: Believes an unrestricted deep tree generalises best
   D) limit maximum depth or minimum samples per leaf  <- CORRECT

**37. [ml.bias_variance.q4] (difficulty 3)** A linear model on clearly curved data mostly suffers from
   A) high bias  <- CORRECT
   B) high variance  <- reveals: Confuses high bias with overfitting
   C) data leakage  <- reveals: Confuses high bias with overfitting
   D) too much data  <- reveals: Confuses high bias with overfitting

**38. [ml.metrics_reg.q2] (difficulty 2)** Which metric penalises large errors most strongly?
   A) MAE  <- reveals: Believes MAE penalises large errors more than MSE does
   B) MSE  <- CORRECT
   C) median error  <- reveals: Believes MAE penalises large errors more than MSE does
   D) count of errors  <- reveals: Believes MAE penalises large errors more than MSE does

**39. [ml.optimizers.q4] (difficulty 4)** Gradient clipping is used to
   A) prevent exploding gradients  <- CORRECT
   B) speed up inference  <- reveals: Believes Adam makes learning rate tuning unnecessary
   C) reduce the number of layers  <- reveals: Believes Adam makes learning rate tuning unnecessary
   D) prevent vanishing gradients  <- reveals: Believes Adam makes learning rate tuning unnecessary

**40. [ml.train_test.q1] (difficulty 1)** The test set should be used
   A) repeatedly to tune hyperparameters  <- reveals: Evaluates the model on the training data and reports that accuracy
   B) for training  <- reveals: Evaluates the model on the training data and reports that accuracy
   C) to fit the scaler  <- reveals: Fits the scaler or imputer on the whole dataset before splitting
   D) once, at the end, for the final estimate  <- CORRECT

## Skill reference notes to review (same seed, 8 skills)

### ml.quantization - Quantization and model compression
Prerequisites: ml.nn_basics, ml.llm_basics; difficulty 4; 4.0 h

Quantization stores weights, and sometimes activations, with fewer bits than 32-bit floats, for example 8-bit or 4-bit integers, shrinking model size and memory traffic and often speeding up inference on hardware with fast integer arithmetic. A scale factor, and sometimes a zero point, maps integers back to real values. Weight-only quantization keeps activations in higher precision; quantizing both weights and activations, such as 4-bit weights with 8-bit activations, can be faster still. Post-training quantization converts a trained model with little or no retraining, while quantization-aware training simulates low precision during training to recover accuracy. Lower precision saves memory roughly proportionally: a 1 billion parameter model needs about 4 GB in 32-bit and about 0.5 GB in 4-bit, plus overhead for the KV cache and activations. Quantization introduces error, so measure quality on your tasks. Other compression methods are pruning, distillation and smaller architectures.

### ml.stats_basics - Probability and statistics basics
Prerequisites: none; difficulty 2; 4.0 h

Machine learning rests on probability. A probability lies between 0 and 1 and the probabilities of all outcomes sum to 1. Conditional probability P(A given B) is the chance of A when B is known to have happened, and Bayes' rule links it to the reverse: P(A given B) equals P(B given A) times P(A) divided by P(B). Two events are independent when P(A and B) equals P(A) times P(B). The expected value is the probability-weighted average, and variance measures spread around the mean; the standard deviation is its square root. A normal distribution is described by its mean and standard deviation, with about 68 percent of values within one standard deviation. The law of large numbers says sample averages approach the true mean. A small sample can mislead, so report uncertainty rather than a single number.

### ml.backprop - Backpropagation
Prerequisites: ml.nn_basics, ml.linear_algebra; difficulty 4; 5.0 h

Backpropagation computes the gradient of the loss with respect to every weight efficiently using the chain rule. First the forward pass computes activations layer by layer and stores them; then the backward pass starts from the loss, computes the gradient with respect to the output, and propagates it backward, multiplying local derivatives at each layer to get gradients for the weights and the inputs of the previous layer. This reuses intermediate results, so the cost is about twice that of the forward pass rather than recomputing each derivative separately. Frameworks implement this as automatic differentiation over a computational graph. Gradients can vanish when many small derivatives multiply, as with sigmoid and tanh in deep networks, or explode when derivatives are large; ReLU, careful initialisation, residual connections and gradient clipping help. Backpropagation computes gradients only; an optimiser then uses them to update weights.

### ml.imbalanced - Class imbalance
Prerequisites: ml.metrics_cls, ml.feature_eng; difficulty 3; 3.0 h

In many problems, such as fraud, disease or defect detection, the positive class is rare. A classifier trained naively tends to favour the majority class and accuracy hides the failure, so rely on precision, recall, F1 or the precision-recall curve, which is more informative than ROC under heavy imbalance. Remedies work at the data level or the algorithm level. Data-level methods are random oversampling of the minority, undersampling of the majority, and synthetic oversampling such as SMOTE. Algorithm-level methods are class weights that make minority errors cost more, threshold tuning, and anomaly detection when positives are extremely rare. Always resample only the training data after splitting; resampling before the split leaks duplicates into the validation set and inflates scores. Keep the test set with its natural distribution so the metrics reflect reality. Collecting more minority examples is best.

### ml.trees - Decision trees
Prerequisites: ml.metrics_cls, ml.bias_variance; difficulty 3; 3.0 h

A decision tree predicts by asking a sequence of yes-no questions about features, splitting the data into purer and purer groups until it reaches leaves that give a prediction. At each node the algorithm picks the split that most reduces impurity, measured for classification by Gini impurity or entropy and for regression by variance. Trees need no feature scaling, handle mixed feature types and are easy to interpret as rules. An unrestricted tree keeps splitting until it memorises the training data, so it overfits badly with high variance. Control complexity with maximum depth, minimum samples per leaf and pruning. Small changes in the data can produce a very different tree, so single trees are unstable. Trees split on axis-parallel thresholds, so they approximate diagonal boundaries with staircases. They are the building block of random forests and boosting.

### ml.optimizers - Optimizers and training practice
Prerequisites: ml.backprop, ml.cross_validation; difficulty 4; 4.0 h

Optimisers decide how gradients become weight updates. Plain stochastic gradient descent uses a fixed learning rate; momentum accumulates a velocity of past gradients to smooth noise and speed up along consistent directions; RMSProp and Adam adapt a per-parameter step size using running estimates of gradient magnitude, and Adam also uses momentum, making it a strong default that needs less tuning. The learning rate is the single most important hyperparameter, and schedules such as warm-up followed by cosine or step decay often help. Batch size affects gradient noise, speed and memory: larger batches are efficient on hardware but may generalise slightly worse without learning rate adjustments. Use early stopping on validation loss, dropout and weight decay against overfitting, and gradient clipping against exploding gradients. Track training and validation curves, set random seeds for reproducibility, and overfit a tiny batch first to catch bugs.

### ml.serving - Deploying and serving models
Prerequisites: ml.pipelines, ml.quantization; difficulty 4; 4.0 h

Serving a model means making its predictions available to an application, either as a remote service behind an API or embedded in the app itself. Key concerns are latency, throughput, memory, cost, reliability and privacy. Batch inference scores many items offline on a schedule, while online inference answers single requests in milliseconds to seconds. Export the model into a portable format, such as ONNX, TorchScript or ExecuTorch, and keep the preprocessing identical to training, because training-serving skew is a frequent cause of silent failures. Warm up the model before the first request, since loading and first-run compilation add delay, and cap input sizes. Cache frequent results, batch concurrent requests for throughput and set timeouts and fallbacks so failures degrade gracefully. On-device serving removes network latency and keeps data private but is limited by memory, battery and heat. Version models and support rollback.

### ml.logistic - Logistic regression
Prerequisites: ml.linear_reg, ml.gradient_descent, ml.metrics_cls; difficulty 3; 4.0 h

Logistic regression is a linear classifier. It computes a weighted sum of the features and passes it through the sigmoid function, which squashes any real number into the range 0 to 1 and is interpreted as the probability of the positive class. A threshold, commonly 0.5, converts the probability to a class label and can be moved to trade precision against recall. The decision boundary is linear in the features. Training minimises the cross-entropy, or log loss, which punishes confident wrong predictions very strongly, using gradient descent. The model is called regression for historical reasons but it solves classification. For more than two classes use softmax, which turns scores into probabilities that sum to one. Weights can be read as changes in log-odds. Regularisation is usually added, and features should be scaled for stable training.
