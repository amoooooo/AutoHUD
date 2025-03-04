package mod.crend.autohud.forge.mixin.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mod.crend.autohud.AutoHud;
import mod.crend.autohud.component.Component;
import mod.crend.autohud.component.Hud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.StatusEffectSpriteManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.JumpingMount;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Mixin(InGameHud.class)
@Debug(export = true)
public class InGameHudMixin {
	// Hotbar
	@Inject(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", ordinal = 0))
	private void autoHud$preHotbar(float tickDelta, MatrixStack matrixStack, CallbackInfo ci) {
		Hud.injectTransparency();
	}

	// Hotbar items
	@WrapOperation(
			method = "renderHotbar",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHotbarItem(Lnet/minecraft/client/util/math/MatrixStack;IIFLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;I)V"
			)
	)
	private void autoHud$transparentHotbarItems(InGameHud instance, MatrixStack matrixStack, int x, int y, float tickDelta, PlayerEntity player, ItemStack stack, int seed, Operation<Void> original) {
		if (AutoHud.targetHotbar && AutoHud.config.animationFade()) {
			// Don't render items if they're fully invisible anyway
			if (!Component.Hotbar.fullyHidden() || AutoHud.config.getHotbarItemsMaximumFade() > 0.0f) {
				// We need to reset the renderer because otherwise the first item gets drawn with double alpha
				Hud.postInjectFade();
				// Setup custom framebuffer
				Hud.prepareExtraFramebuffer();
				// Have the original call draw onto the custom framebuffer
				original.call(instance, matrixStack, x, y, tickDelta, player, stack, seed);
				// Render the contents of the custom framebuffer as a texture with transparency onto the main framebuffer
				Hud.preInjectFade(matrixStack, Component.Hotbar, AutoHud.config.getHotbarItemsMaximumFade());
				Hud.drawExtraFramebuffer(matrixStack);
				Hud.postInjectFade(matrixStack);
			}
		} else {
			original.call(instance, matrixStack, x, y, tickDelta, player, stack, seed);
		}
	}

	// Scoreboard
	// Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/text/Text;FFI)I
	@ModifyArg(method = "renderScoreboardSidebar", at=@At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/text/Text;FFI)I"), index = 4)
	private int autoHud$scoreboardSidebarString(int color) {
		if (Hud.inRender) {
			return Hud.getArgb() | 0xFFFFFF;
		}
		return color;
	}
	@ModifyArg(method = "renderScoreboardSidebar", at=@At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/client/util/math/MatrixStack;Ljava/lang/String;FFI)I"), index = 4)
	private int autoHud$scoreboardSidebarText(int color) {
		if (Hud.inRender) {
			return Hud.getArgb() | 0xFFFFFF;
		}
		return color;
	}
	@ModifyArg(method = "renderScoreboardSidebar", at=@At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;fill(Lnet/minecraft/client/util/math/MatrixStack;IIIII)V"), index=5)
	private int autoHud$scoreboardSidebarFill(int color) {
		if (Hud.inRender) {
			return Hud.modifyArgb(color);
		}
		return color;
	}

	// Mount
	@Inject(method = "renderMountJumpBar", at=@At("HEAD"))
	private void autoHud$preMountJumpBar(JumpingMount mount, MatrixStack matrixStack, int x, CallbackInfo ci) {
		if (AutoHud.targetStatusBars) {
			Hud.preInject(matrixStack, Component.MountJumpBar);
		}
	}
	@Inject(method = "renderMountJumpBar", at=@At("RETURN"))
	private void autoHud$postMountJumpBar(JumpingMount mount, MatrixStack matrixStack, int x, CallbackInfo ci) {
		if (AutoHud.targetStatusBars) {
			Hud.postInject(matrixStack);
		}
	}

	// Experience bar
	@Inject(method = "renderExperienceBar", at=@At("HEAD"))
	private void autoHud$preExperienceBar(MatrixStack matrixStack, int x, CallbackInfo ci) {
		if (AutoHud.targetExperienceBar) {
			Hud.preInject(matrixStack, Component.ExperienceBar);
		}
	}
	@Inject(method = "renderExperienceBar", at=@At("RETURN"))
	private void autoHud$postExperienceBar(MatrixStack matrixStack, int x, CallbackInfo ci) {
		if (AutoHud.targetExperienceBar) {
			Hud.postInject(matrixStack);
		}
	}

	// Status Effects
	@Inject(method = "renderStatusEffectOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/effect/StatusEffectInstance;getEffectType()Lnet/minecraft/entity/effect/StatusEffect;", ordinal = 0), locals = LocalCapture.CAPTURE_FAILSOFT)
	private void autoHud$preEffect(MatrixStack matrixStack, CallbackInfo ci, Collection<StatusEffectInstance> collection, Screen screen, int i, int j, StatusEffectSpriteManager statusEffectSpriteManager, List<Runnable> list, Iterator<StatusEffectInstance> var7, StatusEffectInstance statusEffectInstance) {
		if (AutoHud.targetStatusEffects && Hud.shouldShowIcon(statusEffectInstance)) {
			Hud.preInject(matrixStack, Component.get(statusEffectInstance.getEffectType()));
		}
	}
	@Inject(method = "renderStatusEffectOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/StatusEffectSpriteManager;getSprite(Lnet/minecraft/entity/effect/StatusEffect;)Lnet/minecraft/client/texture/Sprite;"))
	private void autoHud$postEffect(MatrixStack matrixStack, CallbackInfo ci) {
		if (AutoHud.targetStatusEffects) {
			Hud.postInject(matrixStack);
		}
	}
	@Inject(method = {"m_279741_", "method_18620"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;drawSprite(Lnet/minecraft/client/util/math/MatrixStack;IIIIILnet/minecraft/client/texture/Sprite;)V"), require = 0)
	private static void autoHud$preSprite(Sprite sprite, float f, MatrixStack matrixStack, int i, int j, CallbackInfo ci) {
		if (AutoHud.targetStatusEffects) {
			Component component = Component.findBySprite(sprite);
			if (component != null) {
				Hud.preInject(matrixStack, component);
			} else {
				matrixStack.push();
			}
		}
	}
	@Inject(method = {"m_279741_", "method_18620"}, at = @At(value = "RETURN"), require = 0)
	private static void autoHud$postSprite(Sprite sprite, float f, MatrixStack matrixStack, int i, int j, CallbackInfo ci) {
		if (AutoHud.targetStatusEffects) {
			Hud.postInject(matrixStack);
		}
	}

	@Redirect(method = "renderStatusEffectOverlay", at = @At(value = "INVOKE", target="Lnet/minecraft/entity/effect/StatusEffectInstance;shouldShowIcon()Z"))
	private boolean autoHud$shouldShowIconProxy(StatusEffectInstance instance) {
		return Hud.shouldShowIcon(instance);
	}
}
