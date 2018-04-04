/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.workspace.server;

import java.util.concurrent.locks.Lock;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;
import org.jgroups.JChannel;
import org.jgroups.blocks.ReplicatedHashMap;
import org.jgroups.blocks.locking.LockService;

/** @author Anton Korneta */
@Singleton
public class ReplicatedWorkspaceStatusCache implements WorkspaceStatusCache {

  private static final String CHANNEL_NAME = "WorkspaceState";

  private final ReplicatedHashMap<String, WorkspaceStatus> delegate;
  private final LockService lockService;

  @Inject
  public ReplicatedWorkspaceStatusCache(@Named("jgroups.config.file") String confFile) {
    try {
      JChannel channel = new JChannel(confFile);
      this.lockService = new LockService(channel);
      channel.connect(CHANNEL_NAME);
      delegate = new ReplicatedHashMap<>(channel);
      delegate.setBlockingUpdates(true);
      delegate.start(5000);
    } catch (Exception ex) {
      throw new RuntimeException("Jgroups cache creation failed. Cause :" + ex.getMessage());
    }
  }

  @Override
  public WorkspaceStatus get(String workspaceId) {
    final Lock lock = lockService.getLock(workspaceId);
    lock.lock();
    try {
      return delegate.get(workspaceId);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public WorkspaceStatus replace(String workspaceId, WorkspaceStatus newStatus) {
    final Lock lock = lockService.getLock(workspaceId);
    lock.lock();
    try {
      return delegate.replace(workspaceId, newStatus);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean replace(
      String workspaceId, WorkspaceStatus prevStatus, WorkspaceStatus newStatus) {
    final Lock lock = lockService.getLock(workspaceId);
    lock.lock();
    try {
      return delegate.replace(workspaceId, prevStatus, newStatus);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public WorkspaceStatus remove(String workspaceId) {
    final Lock lock = lockService.getLock(workspaceId);
    lock.lock();
    try {
      return delegate.remove(workspaceId);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public WorkspaceStatus putIfAbsent(String workspaceId, WorkspaceStatus status) {
    final Lock lock = lockService.getLock(workspaceId);
    lock.lock();
    try {
      return delegate.putIfAbsent(workspaceId, status);
    } finally {
      lock.unlock();
    }
  }
}
