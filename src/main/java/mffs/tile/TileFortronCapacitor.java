package mffs.tile;

import icbm.api.IBlockFrequency;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import mffs.MFFSHelper;
import mffs.ModularForceFieldSystem;
import mffs.TransferMode;
import mffs.api.card.ICard;
import mffs.api.card.ICardInfinite;
import mffs.api.card.ICoordLink;
import mffs.api.fortron.FrequencyGrid;
import mffs.api.fortron.IFortronCapacitor;
import mffs.api.fortron.IFortronFrequency;
import mffs.api.fortron.IFortronStorage;
import mffs.api.modules.IModule;
import mffs.base.TileModuleAcceptor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import universalelectricity.api.vector.Vector3;

import com.google.common.io.ByteArrayDataInput;

public class TileFortronCapacitor extends TileModuleAcceptor implements IFortronStorage, IFortronCapacitor
{
	private TransferMode transferMode = TransferMode.EQUALIZE;

	public TileFortronCapacitor()
	{
		this.capacityBase = 700;
		this.capacityBoost = 10;
		this.startModuleIndex = 2;
	}

	@Override
	public void updateEntity()
	{
		super.updateEntity();

		this.consumeCost();

		/**
		 * Transmit Fortrons in frequency network, evenly distributing them.
		 */
		/**
		 * Gets the card.
		 */
		if (this.isActive() && this.ticks % 10 == 0)
		{
			Set<IFortronFrequency> machines = new HashSet<IFortronFrequency>();

			for (ItemStack itemStack : this.getCards())
			{
				if (itemStack != null)
				{
					if (itemStack.getItem() instanceof ICardInfinite)
					{
						this.setFortronEnergy(this.getFortronCapacity());
					}
					else if (itemStack.getItem() instanceof ICoordLink)
					{
						Vector3 linkPosition = ((ICoordLink) itemStack.getItem()).getLink(itemStack);

						if (linkPosition != null && linkPosition.getTileEntity(this.worldObj) instanceof IFortronFrequency)
						{
							machines.add(this);
							machines.add((IFortronFrequency) linkPosition.getTileEntity(this.worldObj));
						}
					}
				}
			}

			if (machines.size() < 1)
			{
				machines = this.getLinkedDevices();
			}

			MFFSHelper.transferFortron(this, machines, this.transferMode, this.getTransmissionRate());
		}
	}

	@Override
	public float getAmplifier()
	{
		return 0.001f;
	}

	/**
	 * Packet Methods
	 */
	@Override
	public ArrayList getPacketData(int packetID)
	{
		ArrayList objects = super.getPacketData(packetID);
		objects.add(this.transferMode.ordinal());
		return objects;
	}

	@Override
	public void onReceivePacket(int packetID, ByteArrayDataInput dataStream) throws IOException
	{
		super.onReceivePacket(packetID, dataStream);

		if (packetID == TilePacketType.DESCRIPTION.ordinal())
		{
			this.transferMode = TransferMode.values()[dataStream.readInt()];
		}
		else if (packetID == TilePacketType.TOGGLE_MODE.ordinal())
		{
			this.transferMode = this.transferMode.toggle();
		}
	}

	@Override
	public int getSizeInventory()
	{
		return 5;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		this.transferMode = TransferMode.values()[nbt.getInteger("transferMode")];
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound)
	{
		super.writeToNBT(nbttagcompound);
		nbttagcompound.setInteger("transferMode", this.transferMode.ordinal());
	}

	@Override
	public Set<IFortronFrequency> getLinkedDevices()
	{
		Set<IFortronFrequency> fortronBlocks = new HashSet<IFortronFrequency>();
		Set<IBlockFrequency> frequencyBlocks = FrequencyGrid.instance().get(this.worldObj, new Vector3(this), this.getTransmissionRange(), this.getFrequency());

		for (IBlockFrequency frequencyBlock : frequencyBlocks)
		{
			if (frequencyBlock instanceof IFortronFrequency)
			{
				fortronBlocks.add((IFortronFrequency) frequencyBlock);
			}
		}

		return fortronBlocks;
	}

	@Override
	public boolean isItemValidForSlot(int slotID, ItemStack itemStack)
	{
		if (slotID == 0 || slotID == 1)
		{
			return itemStack.getItem() instanceof ICard;
		}
		else
		{
			return itemStack.getItem() instanceof IModule;
		}
	}

	@Override
	public Set<ItemStack> getCards()
	{
		Set<ItemStack> cards = new HashSet<ItemStack>();
		cards.add(super.getCard());
		cards.add(this.getStackInSlot(1));
		return cards;
	}

	public TransferMode getTransferMode()
	{
		return this.transferMode;
	}

	@Override
	public int getTransmissionRange()
	{
		return 15 + this.getModuleCount(ModularForceFieldSystem.itemModuleScale);
	}

	@Override
	public int getTransmissionRate()
	{
		return 250 + 50 * this.getModuleCount(ModularForceFieldSystem.itemModuleSpeed);
	}
}