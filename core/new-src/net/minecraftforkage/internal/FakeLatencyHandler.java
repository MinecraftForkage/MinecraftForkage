package net.minecraftforkage.internal;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

/**
 * Delays all incoming packets by a fixed time period. Somewhat hacky, but
 * only used for testing anyway.
 * 
 * @author immibis
 */
public class FakeLatencyHandler extends ChannelInboundHandlerAdapter
{
    private static final Timer timer = new HashedWheelTimer();
    
    public static final int LATENCY_MS;
    static {
    	Integer i = Integer.getInteger("net.mcforkage.fakeLatency");
    	LATENCY_MS = (i == null ? 0 : i.intValue());
    }
    
    private abstract static class Task implements TimerTask {
    	@Override
    	public final void run(Timeout arg0) throws Exception {
    		synchronized(tasks) {
    			tasks.poll().actuallyRun();
    		}
    	}
    	abstract void actuallyRun();
    }
    
    private void addTask(Task task) {
    	synchronized(tasks) {
    		tasks.add(task);
    	}
    	timer.newTimeout(task, LATENCY_MS, TimeUnit.MILLISECONDS);
    }
    
    private static final Queue<Task> tasks = new LinkedList<Task>();
    
    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
    	addTask(new Task() {
            @Override void actuallyRun() {
            	ctx.fireChannelRead(msg);
            }
    	});
    }
    
    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) throws Exception {
    	addTask(new Task() {
            @Override void actuallyRun() {
                ctx.fireChannelReadComplete();
            }
    	});
    }
    
    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
    	// Don't delay this
    	ctx.fireChannelActive();
    }
    
    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
    	addTask(new Task() {
            @Override void actuallyRun() {
                ctx.fireChannelInactive();
            }
    	});
    }

	public static boolean isEnabled() {
		return System.getProperty("net.mcforkage.fakeLatency") != null;
	}
}