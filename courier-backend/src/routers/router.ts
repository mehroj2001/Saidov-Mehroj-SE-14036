import { Router } from "express";
import { body } from "express-validator";
import { addShop, deleteShop, getShops } from "../handlers/shop";

const router = Router();

/**
 * Product Routes
 */

router.get(
    '/getShops',
    getShops
)

router.post(
    '/addShop',
    body('name').isString().notEmpty(),
    body('latitude').isFloat().notEmpty(),
    body('longitude').isFloat().notEmpty(),
    body('contact_person').isString().notEmpty(),
    body('phone').isString().notEmpty(),
    body('isEnabled').isBoolean().notEmpty(),
    addShop
)

router.delete(
    '/deleteShop',
    deleteShop,
)

export default router