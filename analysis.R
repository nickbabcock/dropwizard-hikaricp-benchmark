library(tidyverse)
library(readr)
library(knitr)

columns <- c("config", "pool size", "max threads", "requests", "mean", "stdev", "p50", "p90", "p99")

df <- read_csv("wrk-100.csv", col_names = columns)

# Divid the microseconds by 1000 to transform into milliseconds which are more intuitive.
# Update max threads to the number of threads that will be used to service requests
df <- mutate(df, 
             mean = mean / 1000,
             stdev = stdev / 1000,
             p50 = p50 / 1000,
             p90 = p90 / 1000,
             p99 = p99 / 1000,
             `max threads` = `max threads` - 6)

# A pool size that is bigger than the number of max requests doesn't really make sense
# because then there will always be some connections not being used. The data should
# already be cleaned of this logic, but just in case, we'll replicate it here
df <- filter(df, `pool size` <= `max threads`)

# Grab the top five configurations (that maximizes number of requests) from tomcat
# and hikari for some further analysis
top_5 <- df %>% group_by(config) %>%
  top_n(n = 5, wt = requests) %>%
  arrange(-requests)

requests_plots <- function(m_d, subtitle) {
  ggplot(gather(m_d, percentile, response, p50, p90, p99), 
         aes(config, response, ymin=0, fill=percentile)) +
    geom_jitter(size=4, width=0.15, shape=21) +
    xlab("") + ylab("Response latency (ms)") +
    ggtitle("Response Latencies", subtitle = subtitle)
  
  ggplot(m_d, aes(x = factor(0), y = requests, fill=config, ymin=0)) +
    geom_jitter(size=4, width=0.15, shape=21) +
    xlab("") + ylab("Request Throughput") +
    scale_x_discrete(breaks = NULL) + coord_flip() +
    ggtitle("Request Throughput", subtitle = subtitle)  
}

requests_plots(top_5, "For top 5 configurations by throughput for each pool")
requests_plots(df, "For all configurations")

kable(top_5 %>% filter(config == 'hikari'), "markdown")
kable(top_5 %>% filter(config == 'tomcat'), "markdown")
