library(GGally)
data <- read.table("/home/adutta/git/OIE-Integration/KCL_MCL_CL2", sep="\t", header=TRUE)
ggpairs(log(data), lower=list(continuous="smooth", params=c(colour="blue")), diag=list(continuous="bar", params=c(colour="blue")),upper=list(params=list(corSize=6)), axisLabels="internal")

library(ggplot2)
data <- read.table("/home/adutta/git/OIE-Integration/KCL_MCL_CL2", sep="\t", header=TRUE)
g<-ggplot(data, aes(x=data$CLUSTER_SIZE))
g<-g+ geom_line(aes(y=log(data$KCL_SCORE)), colour='red')
g<-g+ geom_line(aes(y=log(data$MCL_SCORE)), colour='blue')
g<-g+ ylab("Cluster Scores (log-scale)")
g<-g+ xlab("Cluster Size")
g<-g+ ggtitle("Cluster Size vs Cluster scores of the different methods")
g<-g+scale_fill_continuous(guide="legend")
g