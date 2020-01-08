/*
 *  Copyright 2019 <---> Present Status Machina Contributors (https://github.com/entzik/status-machina/graphs/contributors)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 *
 */

package io.statusmachina.core.spi;

import io.statusmachina.core.api.Machine;

import java.util.concurrent.Callable;

public interface MachinePersistenceCallback<S,E> {
    Machine<S,E> saveNew(Machine<S,E> machine);
    Machine<S,E> update(Machine<S,E> machine);
    <R> R runInTransaction(Callable<R> callable) throws Exception;
}