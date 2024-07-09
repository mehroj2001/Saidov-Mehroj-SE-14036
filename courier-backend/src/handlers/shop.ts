import { validationResult } from "express-validator";
import { Request, Response } from "express";
import prisma from "../database/db";

export const getShops = async (req: any, res: any) => {
    const shops = await prisma.shop.findMany({});
    return res.status(200).json(
        {
            message: 'Success',
            shops: shops,
        }
    );
}

export const addShop = async (req: any, res: any) => {
    try {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({ error: errors.array() });
        }

        const shop = await prisma.shop.create({
            data: {
                name: req.body.name,
                contact_person: req.body.contact_person,
                latitude: req.body.latitude,
                longitude: req.body.longitude,
                phone: req.body.phone,
                isEnabled: req.body.isEnabled,
            }
        });

        return res.status(201).json({
            message: 'Successfully created',
            newShop: shop,
        });
    } catch (e) {
        return res.status(404).json({
            message: 'Error occured',
        });

    }

}


export const deleteShop = async (req: any, res: any) => {
    try {
        const shop = await prisma.shop.findFirst({
            where: {
                id: +req.query.id,
            }
        });

        if (!shop) {
            return res.status(404).json({
                message: 'Shop not found',
            });
        }

        await prisma.shop.delete({
            where: {
                id: +req.query.id,
            }
        });


        return res.status(201).json({
            message: 'Successfully deleted',
            shop: shop,
        });
    } catch (e) {
        return res.status(404).json({
            message: 'Error occured',
        });
    }

}