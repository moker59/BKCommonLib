package com.bergerkiller.bukkit.common.wrappers;

import java.util.Collection;

import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.generated.net.minecraft.server.IBlockStateHandle;

/**
 * A single state of a block
 */
public class BlockState<T extends Comparable<?>> extends BasicWrapper<IBlockStateHandle> {

    public BlockState(IBlockStateHandle handle) {
        setHandle(handle);
    }

    /**
     * Name identifying this state
     * 
     * @return name
     */
    public String name() {
        return handle.getKeyToken();
    }

    /**
     * The possible values this state can have
     * 
     * @return values
     */
    public Collection<T> values() {
        return CommonUtil.unsafeCast(handle.getValues());
    }

    /**
     * Gets a String representation of a value returned by {@link #values()}
     * 
     * @param value
     * @return value name
     */
    public String valueName(Comparable<?> value) {
        return handle.getValueToken(value);
    }
}
