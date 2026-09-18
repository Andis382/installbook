<?php

namespace App\Http\Controllers\Concerns;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Http\Request;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;

/**
 * Every record in this application belongs to exactly one installer. Rather than trust
 * each controller to remember a where clause, anything reached by id goes through here,
 * and a record belonging to somebody else is indistinguishable from one that does not
 * exist: the same 404, with nothing leaked about whether the id is real.
 */
trait ScopesToInstaller
{
    protected function mine(Request $request, Model $model, string $column = 'user_id'): Model
    {
        if ((int) $model->getAttribute($column) !== (int) $request->user()->id) {
            throw new NotFoundHttpException;
        }

        return $model;
    }
}
