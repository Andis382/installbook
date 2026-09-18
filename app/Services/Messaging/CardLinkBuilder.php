<?php

namespace App\Services\Messaging;

use App\Models\Installation;

class CardLinkBuilder
{
    public function for(Installation $installation): string
    {
        return route('card.show', ['token' => $installation->public_token]);
    }

    public function booking(Installation $installation): string
    {
        return route('card.book', ['token' => $installation->public_token]);
    }
}
