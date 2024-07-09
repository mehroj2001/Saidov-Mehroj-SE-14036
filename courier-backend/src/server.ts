import express, { application } from 'express';
import { body, validationResult } from 'express-validator';
import morgan from 'morgan';
import router from './routers/router';
var bodyParser = require('body-parser');
require('dotenv').config();


const app = express();

app.use(morgan('dev'));

app.use(bodyParser.urlencoded({ extended: false }))

app.use(bodyParser.json())

app.use('/api', router);

app.use((req, res, next) => {
    res.status(404).json({
        message: 'There is no such endpoint',
    });
    next();
})

export default app;