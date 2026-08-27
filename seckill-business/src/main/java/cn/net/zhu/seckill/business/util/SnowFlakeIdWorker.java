package cn.net.zhu.seckill.business.util;

/**
 *  雪花分布式ID 工作节点 工具类
 *
 * @author 一只朱
 * @date 2026-08-23 15:21
 *
 * "Run the code. Run the world."
 */

public class SnowFlakeIdWorker {
    //起始时间戳（2020-01-01）
    private final long twepoch = 1577808000000L;
    //机器ID占5位，数据中心ID占5位
    private final long workerIdBits = 5L;
    private final long datacenterIdBits = 5L;
    private final long maxWorkerId = ~(-1L << workerIdBits);
    private final long maxDatacenterId = ~(-1L << datacenterIdBits);
    private final long sequenceBits = 12L;
    private final long workerIdShift = sequenceBits;
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    private final long sequenceMask = ~(-1L << sequenceBits);

    private long workerId;
    private long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowFlakeIdWorker(long workerId,long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0)
            throw new IllegalArgumentException("workerId超出范围");
        if (datacenterId > maxDatacenterId || datacenterId < 0)
            throw new IllegalArgumentException("datacenterId超出范围");
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }
    public synchronized long nextId(){
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp)
            throw new RuntimeException("时钟回拨，拒绝生成ID");
        if (lastTimestamp == timestamp){
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0)
                timestamp = tilNextMillis(lastTimestamp);
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - twepoch) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp)
            timestamp = System.currentTimeMillis();
        return timestamp;
    }


}
