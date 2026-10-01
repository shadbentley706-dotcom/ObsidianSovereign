package com.obsidiansovereign.item;

import com.obsidiansovereign.entity.ObsidianSovereignEntity;
import com.obsidiansovereign.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Use on Crying Obsidian to summon the Obsidian Sovereign. */
public class SovereignSigilItem extends Item {
    public SovereignSigilItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();

        if (!level.getBlockState(pos).is(Blocks.CRYING_OBSIDIAN)) {
            if (!level.isClientSide && player != null) {
                player.displayClientMessage(Component.translatable("message.obsidiansovereign.sigil_fail")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.FAIL;
        }

        if (level instanceof ServerLevel server) {
            if (server.getDifficulty() == Difficulty.PEACEFUL) {
                if (player != null) {
                    player.displayClientMessage(Component.translatable("message.obsidiansovereign.sigil_peaceful")
                            .withStyle(ChatFormatting.GRAY), true);
                }
                return InteractionResult.FAIL;
            }

            BlockPos spawnPos = pos.above();
            ObsidianSovereignEntity boss = ModEntities.OBSIDIAN_SOVEREIGN.get().create(server);
            if (boss == null) return InteractionResult.FAIL;
            boss.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, ctx.getRotation() + 180.0F, 0.0F);
            server.addFreshEntity(boss);

            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(server);
            if (lightning != null) {
                lightning.moveTo(Vec3.atBottomCenterOf(spawnPos));
                lightning.setVisualOnly(true);
                server.addFreshEntity(lightning);
            }
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, spawnPos.getX() + 0.5D, spawnPos.getY() + 1.5D,
                    spawnPos.getZ() + 0.5D, 120, 1.0D, 1.5D, 1.0D, 0.15D);
            server.playSound(null, spawnPos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 0.7F);

            if (player != null && !player.getAbilities().instabuild) {
                ctx.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.obsidiansovereign.sigil").withStyle(ChatFormatting.LIGHT_PURPLE));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
