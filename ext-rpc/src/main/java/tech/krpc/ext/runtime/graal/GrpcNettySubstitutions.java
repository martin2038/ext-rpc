package tech.krpc.ext.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.grpc.ServerProvider;

@TargetClass(className = "io.grpc.netty.Utils")
final class Target_io_grpc_netty_Utils {

    static {
        //io.grpc.ManagedChannelProvider$ProviderNotFoundException: No functional server found. Try adding a dependency on the grpc-netty or grpc-netty-shaded artifact quarkus graal native
        //https://github.com/quarkusio/quarkus/blob/main/extensions/grpc/runtime/src/main/java/io/quarkus/grpc/spi/GrpcBuilderProvider.java
        System.out.println("[ RPC Server For Graalvm disable Epoll ] Static ServerProvider :" + ServerProvider.provider());
        //see Target_io_netty_util_internal_logging_InternalLoggerFactory
    }

    @Substitute
    static boolean isEpollAvailable() {
        return false;
    }

    @Substitute
    private static Throwable getEpollUnavailabilityCause() {
        return null;
    }
}

@SuppressWarnings("unused")
class GrpcNettySubstitutions {
}