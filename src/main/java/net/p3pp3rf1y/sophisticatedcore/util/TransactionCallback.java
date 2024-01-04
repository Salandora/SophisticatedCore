package net.p3pp3rf1y.sophisticatedcore.util;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

public class TransactionCallback {
    public static void onSuccess(TransactionContext ctx, Runnable r) {
        ctx.addCloseCallback((transaction, result) -> {
            if (result.wasCommitted()) {
                r.run();
            }
        });
    }

	public static void onOuterSuccess(TransactionContext ctx, Runnable r) {
		ctx.addOuterCloseCallback((result) -> {
			if (result.wasCommitted()) {
				r.run();
			}
		});
	}

    public static void onFailed(TransactionContext ctx, Runnable r) {
        ctx.addCloseCallback((transaction, result) -> {
            if (result.wasAborted()) {
                r.run();
            }
        });
    }
}
