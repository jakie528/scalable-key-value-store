#include "db/NodeManager.h"

NodeManager::NodeManager() {
    stop_signal_ = false;
}

void NodeManager::RecvStopReq() {
    {
      lock_guard<mutex> lock(stop_mutex_);
      stop_signal_ = true;
    }
    stop_cv_.notify_all();
}

void NodeManager::WaitUntilStop() {
    unique_lock<mutex> lock(stop_mutex_);
    stop_cv_.wait(lock, [this]{return stop_signal_;});
}