package mapwriter.forge;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import mapwriter.Mw;

/**
 * No description given
 *
 * @author jk-5
 */
public class CloseDetector extends ChannelDuplexHandler {

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise future) throws Exception{
        MwForge.instance.executor = new Runnable(){
            @Override
            public void run(){
                Mw.instance.onConnectionClosed();
            }
        };
        ctx.pipeline().remove(this);
        ctx.close(future);
    }
}
