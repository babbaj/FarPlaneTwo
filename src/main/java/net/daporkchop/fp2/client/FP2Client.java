/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.fp2.client;

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.mode.heightmap.client.HeightmapShaders;
import net.daporkchop.fp2.mode.voxel.client.VoxelShaders;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GLContext;

import static net.daporkchop.fp2.client.ClientConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * Manages initialization of FP2 on the client.
 *
 * @author DaPorkchop_
 */
@UtilityClass
@SideOnly(Side.CLIENT)
public class FP2Client {
    /**
     * Called during {@link FMLPreInitializationEvent}.
     */
    public void preInit() {
        if (!GLContext.getCapabilities().OpenGL45) { //require at least OpenGL 4.5
            unsupported("Your system does not support OpenGL 4.5!");
        }

        int size = glGetInteger(GL_MAX_SHADER_STORAGE_BLOCK_SIZE);
        FP2_LOG.info(PStrings.fastFormat("Max SSBO size: %d bytes (%.2f MiB)", size, size / (1024.0d * 1024.0d)));

        if (!mc.getFramebuffer().isStencilEnabled()) {
            checkState(mc.getFramebuffer().enableStencil(), "unable to enable stencil buffer!");
            FP2_LOG.info("Successfully enabled stencil buffer!");
        }

        MinecraftForge.EVENT_BUS.register(new ClientEvents());

        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(new FP2ResourceReloadListener());
    }

    /**
     * Called during {@link FMLInitializationEvent}.
     */
    public void init() {
        KeyBindings.register();
    }

    /**
     * Called during {@link FMLPostInitializationEvent}.
     */
    public void postInit() {
        TexUVs.initDefault();

        //load shader classes on client thread
        PUnsafe.ensureClassInitialized(HeightmapShaders.class);
        PUnsafe.ensureClassInitialized(VoxelShaders.class);
    }
}
